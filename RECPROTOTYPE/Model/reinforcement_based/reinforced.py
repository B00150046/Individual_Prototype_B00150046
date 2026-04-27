import numpy as np

# =========================================================
# FEATURE FUNCTION (SAFE + CONSISTENT)
# =========================================================

def build_feature_vector(book, user_avg_rating=3.0, user_interaction_count=1):
    return np.array([
        book["avg_rating"] / 5.0,
        np.log1p(book["rating_count"]) / 10.0,
        user_avg_rating / 5.0,
        np.log1p(user_interaction_count) / 5.0
    ])


# =========================================================
# REWARD FUNCTION (FROM USER RATINGS)
# =========================================================

def reward_from_rating(rating):

    try:
        rating = float(rating)
    except:
        return 0.0

   
    return rating / 5.0


# =========================================================
# LINUCB MODEL (STABLE IMPLEMENTATION)
# =========================================================

class LinUCB:

    def __init__(self, n_features=4, alpha=1.0):

        self.alpha = alpha
        self.n_features = n_features

        self.A = {}
        self.b = {}

    # -----------------------------------------------------
    def _init_user(self, user):

        if user not in self.A:
            self.A[user] = np.identity(self.n_features) * 0.1
            self.b[user] = np.zeros((self.n_features, 1))

    # -----------------------------------------------------
    def recommend(self, user, candidates, k=5, user_avg_rating=3.0, user_interaction_count=1):

        self._init_user(user)

        A = self.A[user]
        b = self.b[user]

        A_inv = np.linalg.pinv(A)
        theta = A_inv @ b

        scores = []

        for _, book in candidates.iterrows():

            x = build_feature_vector(book, user_avg_rating, user_interaction_count).reshape(-1, 1)

            exploit = float(theta.T @ x)
            explore = self.alpha * float(np.sqrt(x.T @ A_inv @ x))

            score = exploit + explore

            scores.append((book["Id"], score))

        scores.sort(key=lambda x: x[1], reverse=True)

        return [s[0] for s in scores[:k]]

    # -----------------------------------------------------
    def update(self, user, x, reward):

        self._init_user(user)

        x = x.reshape(-1, 1)

        self.A[user] += x @ x.T
        self.b[user] += reward * x