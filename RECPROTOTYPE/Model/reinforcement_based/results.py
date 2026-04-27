import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
try:
    import seaborn as sns
except ImportError:
    sns = None

import clean
from reinforced import LinUCB, build_feature_vector, reward_from_rating

# =========================================================
# UTILITIES
# =========================================================

def parse_rating_dist(dist):
    if isinstance(dist, str) and ":" in dist:
        return int(dist.split(":")[1])
    return 0


# =========================================================
# LOAD DATA
# =========================================================

# Load books from multiple datasets
big_books1 = pd.read_csv("data/book1-100k.csv")
big_books2 = pd.read_csv("data/kindle_data-v2.csv")

# Standardize columns for big_books1
big_books1 = big_books1.rename(columns={
    "Id": "Id",
    "Name": "Name",
    "RatingDist1": "RatingDist1",
    "RatingDist2": "RatingDist2",
    "RatingDist3": "RatingDist3",
    "RatingDist4": "RatingDist4",
    "RatingDist5": "RatingDist5"
})

# Compute explicit distribution counts for book1 dataset
for i in ["1", "2", "3", "4", "5"]:
    big_books1[f"r{i}"] = big_books1[f"RatingDist{i}"].apply(parse_rating_dist)

# Standardize columns for big_books2
big_books2 = big_books2.rename(columns={
    "asin": "Id",
    "title": "Name",
    "stars": "avg_rating",
    "reviews": "rating_count"
})

# Add placeholder rating distributions for book2 dataset rows
for col in ["r1", "r2", "r3", "r4", "r5"]:
    big_books2[col] = 0

# Combine books without dropping rows globally
big_books = pd.concat([big_books1, big_books2], ignore_index=True)

interactions = pd.read_csv("data/book_users.csv")
interactions.dropna(inplace=True)

# normalize ID types for join operations
interactions["Id"] = interactions["Id"].astype(int).astype(str)
big_books["Id"] = big_books["Id"].astype(str)

# Filter interactions to only those with books
interactions = interactions[interactions["Id"].isin(big_books["Id"])]

user_history = interactions.groupby("UserId")["Id"].apply(list)

user_stats = interactions.groupby("UserId").agg({
    "User_Rating": "mean",
    "Id": "count"
}).rename(columns={"User_Rating": "avg_rating", "Id": "interaction_count"})

# =========================================================
# CLEAN DATA
# =========================================================

def parse_rating_dist(dist):
    if isinstance(dist, str) and ":" in dist:
        return int(dist.split(":")[1])
    return 0


for i in ["1", "2", "3", "4", "5"]:
    if f"r{i}" not in big_books.columns:
        big_books[f"r{i}"] = big_books[f"RatingDist{i}"].apply(parse_rating_dist) if f"RatingDist{i}" in big_books.columns else 0
    else:
        # Fill any missing distribution values with 0 for book2 rows
        big_books[f"r{i}"] = big_books[f"r{i}"].fillna(0)

big_books["rating_count"] = (
    big_books["r1"] + big_books["r2"] +
    big_books["r3"] + big_books["r4"] +
    big_books["r5"]
)

# For books without rating_count, use the provided one (e.g., kindle_data reviews)
big_books["rating_count"] = big_books["rating_count"].where(big_books["rating_count"] > 0, big_books.get("rating_count", 0))

big_books["avg_rating"] = big_books["avg_rating"].fillna((
    1 * big_books["r1"] +
    2 * big_books["r2"] +
    3 * big_books["r3"] +
    4 * big_books["r4"] +
    5 * big_books["r5"]
) / big_books["rating_count"].replace(0, 1))

# If there are still missing avg_rating values, fall back to the 'Rating' column from book1 dataset
if "Rating" in big_books.columns:
    big_books["avg_rating"] = big_books["avg_rating"].fillna(big_books["Rating"])

books = big_books[["Id", "Name", "avg_rating", "rating_count"]].drop_duplicates()

# baseline: top 5 by rating_count
top_books = books.sort_values("rating_count", ascending=False)["Id"].head(5).tolist()

# =========================================================
# INIT MODEL
# =========================================================

linucb = LinUCB(n_features=4)

# =========================================================
# EXPERIMENT
# =========================================================

def run_experiment():

    results = []

    train = interactions.sample(frac=0.8, random_state=42)
    test = interactions.drop(train.index)

    # ---------------- TRAIN ----------------
    for _, row in train.iterrows():

        user = row["UserId"]
        book_id = row["Id"]
        rating = row["User_Rating"]

        match = books[books["Id"] == book_id]
        if match.empty:
            continue

        book = match.iloc[0]

        user_stats_row = user_stats.loc[user]
        x = build_feature_vector(book, user_stats_row["avg_rating"], user_stats_row["interaction_count"])
        reward = reward_from_rating(rating)

        linucb.update(user, x, reward)

    # ---------------- TEST ----------------
    for _, row in test.iterrows():

        user = row["UserId"]
        true_book = row["Id"]

        true_books = user_history.get(user, [])

        # ---------------- LinUCB ----------------
        true_book_id = row["Id"]
        other_books = books[books["Id"] != true_book_id]
        # Include top 50 popular books + random sample
        top_popular = other_books.sort_values("rating_count", ascending=False).head(50)["Id"].tolist()
        remaining = other_books[~other_books["Id"].isin(top_popular)]
        sampled_books = remaining.sample(min(49, len(remaining)), random_state=42)
        candidate_ids = top_popular + sampled_books["Id"].tolist()
        true_book_row = books[books["Id"] == true_book_id]
        candidates = pd.concat([books[books["Id"].isin(candidate_ids)], true_book_row])

        # ---------------- baseline ----------------
        baseline_recs = candidates.sort_values("rating_count", ascending=False)["Id"].head(5).tolist()

        user_stats_row = user_stats.loc[user]
        linucb_recs = linucb.recommend(user, candidates, k=5, user_avg_rating=user_stats_row["avg_rating"], user_interaction_count=user_stats_row["interaction_count"])

        # ---------------- FIXED EVALUATION ----------------
        baseline_hit = int(len(set(baseline_recs) & set(true_books)) > 0)
        linucb_hit = int(len(set(linucb_recs) & set(true_books)) > 0)

        true_rating = row.get("User_Rating", None)
        true_reward = reward_from_rating(true_rating)

        results.append({
            "user": user,
            "true": true_book,
            "true_rating": true_rating,
            "true_reward": true_reward,
            "baseline": baseline_recs,
            "linucb": linucb_recs,
            "baseline_hit": int(true_book in baseline_recs),
            "linucb_hit": int(true_book in linucb_recs)
        })

    return results


# =========================================================
# EVALUATION
# =========================================================

def evaluate(results, k=5):

    baseline_metrics = []
    linucb_metrics = []

    for r in results:

        true_item = r["true"]

        b = r["baseline"]
        l = r["linucb"]

        baseline_metrics.append({
            "precision": precision_at_k(b, true_item, k),
            "recall": recall_at_k(b, true_item, k),
            "f1": f1_at_k(precision_at_k(b, true_item, k), recall_at_k(b, true_item, k)),
            "ndcg": ndcg_at_k(b, true_item, k),
            "map": average_precision(b, true_item, k)
        })

        linucb_metrics.append({
            "precision": precision_at_k(l, true_item, k),
            "recall": recall_at_k(l, true_item, k),
            "f1": f1_at_k(precision_at_k(l, true_item, k), recall_at_k(l, true_item, k)),
            "ndcg": ndcg_at_k(l, true_item, k),
            "map": average_precision(l, true_item, k)
        })

    def avg(m, key):
        return np.mean([x[key] for x in m]) if m else 0.0

    baseline_avg = {
        "precision": avg(baseline_metrics, "precision"),
        "recall": avg(baseline_metrics, "recall"),
        "f1": avg(baseline_metrics, "f1"),
        "ndcg": avg(baseline_metrics, "ndcg"),
        "map": avg(baseline_metrics, "map")
    }

    linucb_avg = {
        "precision": avg(linucb_metrics, "precision"),
        "recall": avg(linucb_metrics, "recall"),
        "f1": avg(linucb_metrics, "f1"),
        "ndcg": avg(linucb_metrics, "ndcg"),
        "map": avg(linucb_metrics, "map")
    }

    print("\n===== BASELINE =====")
    for key, val in baseline_avg.items():
        print(f"{key.capitalize()}@K: {val:.6f}")

    print("\n===== LINUCB =====")
    for key, val in linucb_avg.items():
        print(f"{key.capitalize()}@K: {val:.6f}")

    metrics = list(baseline_avg.keys())
    baseline_vals = list(baseline_avg.values())
    linucb_vals = list(linucb_avg.values())

    if sns is not None:
        sns.set_style('whitegrid')
    else:
        plt.style.use('seaborn-whitegrid')
    x = np.arange(len(metrics))
    width = 0.35

    fig, ax = plt.subplots(figsize=(12, 6))
    ax.bar(x - width/2, baseline_vals, width, label='Baseline', alpha=0.85)
    ax.bar(x + width/2, linucb_vals, width, label='LinUCB', alpha=0.85)

    ax.set_xlabel('Metrics')
    ax.set_ylabel('Score')
    ax.set_title('Offline Evaluation Metrics Comparison')
    ax.set_xticks(x)
    ax.set_xticklabels([m.capitalize() for m in metrics])
    ax.legend()

    for i, v in enumerate(baseline_vals):
        ax.text(i - width/2, v + 0.0003, f"{v:.4f}", ha='center', va='bottom', fontsize=9)
    for i, v in enumerate(linucb_vals):
        ax.text(i + width/2, v + 0.0003, f"{v:.4f}", ha='center', va='bottom', fontsize=9)

    plt.tight_layout()
    plt.savefig('evaluation_metrics.png', dpi=300, bbox_inches='tight')
    # plt.show()  # Commented out to avoid hanging in terminal

    return baseline_avg, linucb_avg

# ---------------------------
# TOP-K METRICS
# ---------------------------

def precision_at_k(recs, true_item, k=5):
    return int(true_item in recs[:k]) / k


def recall_at_k(recs, true_item, k=5):
    return int(true_item in recs[:k])  # single relevant item case


def f1_at_k(p, r):
    return 0 if (p + r) == 0 else 2 * (p * r) / (p + r)


# ---------------------------
# NDCG@K
# ---------------------------
def dcg(recs, true_item, k=5):
    for i, r in enumerate(recs[:k]):
        if r == true_item:
            return 1 / np.log2(i + 2)
    return 0


def ndcg_at_k(recs, true_item, k=5):
    ideal = 1  # only one relevant item
    return dcg(recs, true_item, k) / ideal


# ---------------------------
# MAP (single relevance version)
# ---------------------------
def average_precision(recs, true_item, k=5):
    for i, r in enumerate(recs[:k]):
        if r == true_item:
            return 1 / (i + 1)
    return 0


# =========================================================
# ONLINE / SESSION METRICS

def evaluate_online(results, k=5):
    baseline_clicks = 0
    linucb_clicks = 0
    baseline_reward = 0.0
    linucb_reward = 0.0
    baseline_regret = 0.0
    linucb_regret = 0.0
    user_sessions = {}

    for r in results:
        user = r["user"]
        user_sessions[user] = user_sessions.get(user, 0) + 1

        if r["true"] in r["baseline"]:
            baseline_clicks += 1
            baseline_reward += r["true_reward"]
        else:
            baseline_regret += r["true_reward"]

        if r["true"] in r["linucb"]:
            linucb_clicks += 1
            linucb_reward += r["true_reward"]
        else:
            linucb_regret += r["true_reward"]

    total = len(results)
    avg_session_length = np.mean(list(user_sessions.values())) if user_sessions else 0.0

    baseline_ctr = baseline_clicks / total if total else 0.0
    linucb_ctr = linucb_clicks / total if total else 0.0
    baseline_avg_reward = baseline_reward / total if total else 0.0
    linucb_avg_reward = linucb_reward / total if total else 0.0

    print("\n===== ONLINE / SESSION METRICS =====")
    print(f"Baseline CTR: {baseline_ctr:.4f}")
    print(f"Baseline Cumulative Reward: {baseline_reward:.2f}")
    print(f"Baseline Avg Reward: {baseline_avg_reward:.4f}")
    print(f"Baseline Regret: {baseline_regret:.2f}")
    print(f"\nLinUCB CTR: {linucb_ctr:.4f}")
    print(f"LinUCB Cumulative Reward: {linucb_reward:.2f}")
    print(f"LinUCB Avg Reward: {linucb_avg_reward:.4f}")
    print(f"LinUCB Regret: {linucb_regret:.2f}")
    print(f"Average session length (test interactions/user): {avg_session_length:.2f}")

    if sns is not None:
        sns.set_style('whitegrid')
    else:
        plt.style.use('seaborn-whitegrid')
    fig, axes = plt.subplots(3, 1, figsize=(12, 15), sharex=False)
    width = 0.35

    # CTR comparison
    labels = ['CTR']
    baseline_vals = [baseline_ctr]
    linucb_vals = [linucb_ctr]
    x = np.arange(len(labels))

    axes[0].bar(x - width/2, baseline_vals, width, label='Baseline', alpha=0.85)
    axes[0].bar(x + width/2, linucb_vals, width, label='LinUCB', alpha=0.85)
    axes[0].set_xticks(x)
    axes[0].set_xticklabels(labels)
    axes[0].set_ylabel('CTR')
    axes[0].set_title('Online CTR Comparison')
    axes[0].legend()
    for i, v in enumerate(baseline_vals):
        axes[0].text(i - width/2, v + 0.002, f"{v:.4f}", ha='center', va='bottom', fontsize=9)
    for i, v in enumerate(linucb_vals):
        axes[0].text(i + width/2, v + 0.002, f"{v:.4f}", ha='center', va='bottom', fontsize=9)

    # Average reward comparison
    labels = ['Avg Reward']
    baseline_vals = [baseline_avg_reward]
    linucb_vals = [linucb_avg_reward]
    x = np.arange(len(labels))

    axes[1].bar(x - width/2, baseline_vals, width, label='Baseline', alpha=0.85)
    axes[1].bar(x + width/2, linucb_vals, width, label='LinUCB', alpha=0.85)
    axes[1].set_xticks(x)
    axes[1].set_xticklabels(labels)
    axes[1].set_ylabel('Avg Reward')
    axes[1].set_title('Online Avg Reward Comparison')
    axes[1].legend()
    for i, v in enumerate(baseline_vals):
        axes[1].text(i - width/2, v + max(0.01, v * 0.02), f"{v:.4f}", ha='center', va='bottom', fontsize=9)
    for i, v in enumerate(linucb_vals):
        axes[1].text(i + width/2, v + max(0.01, v * 0.02), f"{v:.4f}", ha='center', va='bottom', fontsize=9)

    # Regret comparison
    labels = ['Regret']
    baseline_vals = [baseline_regret]
    linucb_vals = [linucb_regret]
    x = np.arange(len(labels))

    axes[2].bar(x - width/2, baseline_vals, width, label='Baseline', alpha=0.85)
    axes[2].bar(x + width/2, linucb_vals, width, label='LinUCB', alpha=0.85)
    axes[2].set_xticks(x)
    axes[2].set_xticklabels(labels)
    axes[2].set_ylabel('Regret')
    axes[2].set_title('Online Regret Comparison')
    axes[2].legend()
    for i, v in enumerate(baseline_vals):
        axes[2].text(i - width/2, v + max(1.0, v * 0.02), f"{v:.2f}", ha='center', va='bottom', fontsize=9)
    for i, v in enumerate(linucb_vals):
        axes[2].text(i + width/2, v + max(1.0, v * 0.02), f"{v:.2f}", ha='center', va='bottom', fontsize=9)

    plt.tight_layout()
    plt.savefig('online_metrics.png', dpi=300, bbox_inches='tight')
    # plt.show()  # Commented out to avoid hanging in terminal

    return {
        'baseline_ctr': baseline_ctr,
        'baseline_reward': baseline_reward,
        'baseline_avg_reward': baseline_avg_reward,
        'baseline_regret': baseline_regret,
        'linucb_ctr': linucb_ctr,
        'linucb_reward': linucb_reward,
        'linucb_avg_reward': linucb_avg_reward,
        'linucb_regret': linucb_regret,
        'total_interactions': total,
        'avg_session_length': avg_session_length
    }


# =========================================================
# RUN
# =========================================================

if __name__ == "__main__":
    print("Running full dataset evaluation...")
    results = run_experiment()
    print(f"Total test interactions: {len(results)}")
    offline_metrics = evaluate(results)
    online_metrics = evaluate_online(results)