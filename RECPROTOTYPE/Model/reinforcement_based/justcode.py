import pandas as pd
import numpy as np
from sklearn.metrics.pairwise import cosine_similarity

# -----------------------------
# LOAD DATA FROM CSV ONCE
# -----------------------------
df = pd.read_csv("data2.csv")

# Convert ratings to binary rewards (>=4 -> 1)
df['reward'] = (df['rating'] >= 4).astype(int)

# -----------------------------
# Generate synthetic context features per user-item
# (In a real system, use real item/user features)
# -----------------------------
np.random.seed(0)
df['f1'] = np.random.rand(len(df))
df['f2'] = np.random.rand(len(df))
df['f3'] = np.random.rand(len(df))

# -----------------------------
# CREATE USER-ITEM MATRIX
# -----------------------------
user_item_matrix = df.pivot(index="user_id", columns="item_id", values="rating").fillna(0)
print("User-Item Matrix:\n", user_item_matrix)

# -----------------------------
# COMPUTE USER SIMILARITY
# -----------------------------
user_similarity = cosine_similarity(user_item_matrix)
user_similarity_df = pd.DataFrame(
    user_similarity,
    index=user_item_matrix.index,
    columns=user_item_matrix.index
)

# -----------------------------
# LinUCB Class
# -----------------------------
class LinUCB:
    def __init__(self, n_arms, d, alpha=0.5):
        self.n_arms = n_arms
        self.d = d
        self.alpha = alpha
        self.A = [np.identity(d) for _ in range(n_arms)]
        self.b = [np.zeros((d,1)) for _ in range(n_arms)]
    
    def select_arm(self, contexts):
        p_values = []
        for a in range(self.n_arms):
            A_inv = np.linalg.inv(self.A[a])
            theta = A_inv @ self.b[a]
            x = contexts[a].reshape(-1,1)
            p = float(theta.T @ x + self.alpha * np.sqrt(x.T @ A_inv @ x))
            p_values.append(p)
        return np.argmax(p_values)
    
    def update(self, arm, reward, context):
        x = context.reshape(-1,1)
        self.A[arm] += x @ x.T
        self.b[arm] += reward * x

# -----------------------------
# Map items to arms
# -----------------------------
item_ids = df['item_id'].unique()
item_to_arm = {item:i for i,item in enumerate(item_ids)}
arm_to_item = {i:item for item,i in item_to_arm.items()}

linucb = LinUCB(n_arms=len(item_ids), d=3, alpha=0.5)

# -----------------------------
# RECOMMENDER FUNCTION
# -----------------------------
def recommend(user_id, k=2, top_n=3):
    recommendations = []

    user_rows = df[df['user_id'] == user_id]
    for _, row in user_rows.iterrows():
        user_contexts = []
        for item in item_ids:
            item_row = df[(df['user_id']==row['user_id']) & (df['item_id']==item)]
            if not item_row.empty:
                x = item_row[['f1','f2','f3']].values[0]
            else:
                x = np.random.rand(3)  # unknown item context
            user_contexts.append(x)

        chosen_arm = linucb.select_arm(user_contexts)
        chosen_item = arm_to_item[chosen_arm]

        reward_row = df[(df['user_id']==row['user_id']) & (df['item_id']==chosen_item)]
        reward = int(reward_row['reward'].values[0]) if not reward_row.empty else 0

        linucb.update(chosen_arm, reward, user_contexts[chosen_arm])

        recommendations.append(f"User {row['user_id']} recommended item {chosen_item} -> reward {reward}")

    return recommendations