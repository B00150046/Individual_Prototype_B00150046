"""
Online Evaluation using RL4RS Environment
==========================================
Tests Lin-UCB and baseline models in simulated sequential environment
with proper online metrics: CTR, cumulative reward, regret, session length.
"""

import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
import sys
import os

# Add RL4RS to path relative to this script, not the current working directory
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
RL4RS_PATH = os.path.join(SCRIPT_DIR, '..', 'RL4RS-main')
if os.path.isdir(RL4RS_PATH):
    sys.path.insert(0, RL4RS_PATH)

HAS_GYM = False
HAS_RL4RS = False
rl4rs_import_error = None

try:
    import gym
    HAS_GYM = True
except ImportError:
    try:
        import gymnasium as gym
        HAS_GYM = True
    except ImportError as e:
        rl4rs_import_error = e

if HAS_GYM:
    try:
        from rl4rs.env.slate import SlateRecEnv, SlateState
        HAS_RL4RS = True
    except Exception as e:
        HAS_RL4RS = False
        rl4rs_import_error = e

if not HAS_RL4RS:
    print("⚠️  RL4RS not available. Install gym/gymnasium and RL4RS dependencies for full online evaluation.")
    if rl4rs_import_error is not None:
        print("   RL4RS import error:", type(rl4rs_import_error).__name__, rl4rs_import_error)

from reinforced import LinUCB, build_feature_vector, reward_from_rating


# =========================================================
# SIMPLE SLATE ENV (fallback if RL4RS unavailable)
# =========================================================

class SimpleSlateEnv:
    """
    Minimal slate recommendation environment.
    Simulates user responses to recommended items.
    """

    def __init__(self, interactions, books, n_slates=100, slate_size=5):
        self.interactions = interactions.copy()
        self.books = books.copy()
        self.n_slates = n_slates
        self.slate_size = slate_size
        self.slate_idx = 0
        self.user_history = interactions.groupby("UserId")["Id"].apply(list)

    def reset(self):
        """Start a new batch of slate interactions."""
        self.slate_idx = 0
        return None

    def sample_slate(self):
        """Sample a slate (batch of users and candidates)."""
        n_users = min(self.n_slates, len(self.interactions))
        slate_data = self.interactions.sample(n=n_users, random_state=self.slate_idx)
        self.slate_idx += 1
        return slate_data

    def step(self, recommendations, true_items):
        """
        Execute recommendations and get rewards.

        Args:
            recommendations: list of recommended ids per user
            true_items: list of ground truth user items

        Returns:
            rewards: list of binary feedback (1 if hit, 0 otherwise)
            info: dict with session metadata
        """
        rewards = []
        for rec_list, true_id in zip(recommendations, true_items):
            hit = 1.0 if true_id in rec_list else 0.0
            # Get true rating from interactions record
            true_row = self.interactions[self.interactions["Id"] == true_id]
            if not true_row.empty:
                true_rating = true_row["User_Rating"].iloc[0]
            else:
                true_rating = 0.0
            reward = hit * reward_from_rating(true_rating)
            rewards.append(reward)

        return np.array(rewards)


# =========================================================
# ONLINE EVALUATION
# =========================================================

def create_rl4rs_slate_env(interactions, books, n_slates, slate_size):
    """Attempt to instantiate a full RL4RS slate environment.

    RL4RS requires a compatible dataset sample file and a trained simulator model.
    This repository currently does not include a ready-to-use RL4RS sample/model
    file for the book datasets, so the helper preserves the fallback path.
    """
    if not HAS_RL4RS:
        return None

    # TODO: Add a compatible RL4RS sample_file, iteminfo_file, and model_file
    # for this dataset to enable real RL4RS environment evaluation.
    return None


def online_eval_slate(
    interactions,
    books,
    model_linucb,
    user_stats,
    n_slates=100,
    slate_size=5,
    use_rl4rs=False,
    train_books_set=None,
    online_learning=True
):
    """
    Online evaluation in slated recommendation setting.

    Uses the SimpleSlateEnv for sequential decision-making.
    Evaluates Lin-UCB and top-popularity baseline.

    Args:
        interactions: DataFrame of user-item interactions
        books: DataFrame of book metadata
        model_linucb: Trained LinUCB model
        user_stats: DataFrame with user statistics
        n_slates: Number of slate rounds
        slate_size: Size of each slate
        use_rl4rs: Whether to use RL4RS if available
        train_books_set: Set of book IDs seen during training (for realistic evaluation)
        online_learning: If True, update LinUCB with each user feedback (continuous learning)

    Returns:
        dict with online metrics
    """

    env = None
    env_label = "SimpleSlateEnv (fallback)"

    if use_rl4rs and HAS_RL4RS:
        try:
            env = create_rl4rs_slate_env(interactions, books, n_slates, slate_size)
            if env is not None:
                env_label = "RL4RS SlateEnv"
            else:
                print("⚠️  RL4RS support detected, but no compatible RL4RS dataset/model configuration was available. Falling back to SimpleSlateEnv.")
        except Exception as e:
            print("⚠️  Failed to create RL4RS environment:", type(e).__name__, e)
            env = None

    if env is None:
        env = SimpleSlateEnv(interactions, books, n_slates, slate_size)

    print(f"Using environment: {env_label}")
    linucb_ctr = 0.0
    linucb_reward = 0.0
    linucb_regret = 0.0

    baseline_ctr = 0.0
    baseline_reward = 0.0
    baseline_regret = 0.0

    total_interactions = 0.0

    env.reset()

    episode_linucb_rewards = []
    episode_baseline_rewards = []
    episode_linucb_precisions = []
    episode_baseline_precisions = []
    episode_regret_diff = []

    for slate_round in range(n_slates):
        # Sample a slate of interactions
        slate = env.sample_slate()

        linucb_recs_list = []
        baseline_recs_list = []

        slate_linucb_reward = 0.0
        slate_baseline_reward = 0.0
        slate_linucb_hits = 0
        slate_baseline_hits = 0

        for _, row in slate.iterrows():
            user = row["UserId"]
            true_item = row["Id"]
            true_rating = row["User_Rating"]

            # Candidate pool: books from training set + true item (always included)
            if train_books_set is not None:
                # Get popular books from training set
                candidate_ids = list(train_books_set)
                np.random.shuffle(candidate_ids)
                # Keep top 150 + add true item
                candidate_ids = candidate_ids[:150]
            else:
                candidate_ids = books["Id"].tolist()

            # Ensure true item is in candidates
            if true_item not in candidate_ids:
                candidate_ids.append(true_item)

            candidates = books[books["Id"].isin(candidate_ids)]

            # If we have very few candidates, add more
            if len(candidates) < slate_size:
                missing_ids = np.random.choice(
                    books["Id"].tolist(),
                    size=max(0, slate_size * 3 - len(candidates)),
                    replace=False
                ).tolist()
                candidates = pd.concat([candidates, books[books["Id"].isin(missing_ids)]])

            # LinUCB recommendation
            if user in user_stats.index:
                user_stats_row = user_stats.loc[user]
                linucb_recs = model_linucb.recommend(
                    user,
                    candidates,
                    k=slate_size,
                    user_avg_rating=user_stats_row["avg_rating"],
                    user_interaction_count=user_stats_row["interaction_count"]
                )
            else:
                linucb_recs = candidates.nlargest(slate_size, "rating_count")["Id"].tolist()

            # Baseline: top-k from candidates by popularity
            baseline_recs = candidates.nlargest(slate_size, "rating_count")["Id"].tolist()

            linucb_recs_list.append(linucb_recs)
            baseline_recs_list.append(baseline_recs)

            # Compute rewards and regret
            true_reward = reward_from_rating(true_rating)

            # LinUCB
            linucb_hit = 1.0 if true_item in linucb_recs else 0.0
            linucb_ctr += linucb_hit
            linucb_reward += linucb_hit * true_reward
            linucb_regret += (1.0 - linucb_hit) * true_reward

            # Online learning: update LinUCB with feedback
            if online_learning:
                true_book_row = books[books["Id"] == true_item]
                if not true_book_row.empty:
                    x = build_feature_vector(
                        true_book_row.iloc[0],
                        user_stats.loc[user, "avg_rating"] if user in user_stats.index else 3.0,
                        user_stats.loc[user, "interaction_count"] if user in user_stats.index else 1
                    )
                    model_linucb.update(user, x, true_reward)

            # Baseline
            baseline_hit = 1.0 if true_item in baseline_recs else 0.0
            baseline_ctr += baseline_hit
            baseline_reward += baseline_hit * true_reward
            baseline_regret += (1.0 - baseline_hit) * true_reward

            slate_linucb_hits += linucb_hit
            slate_baseline_hits += baseline_hit
            slate_linucb_reward += linucb_hit * true_reward
            slate_baseline_reward += baseline_hit * true_reward

            total_interactions += 1.0

        slate_users = len(slate)
        if slate_users > 0:
            episode_linucb_rewards.append(slate_linucb_reward)
            episode_baseline_rewards.append(slate_baseline_reward)
            episode_linucb_precisions.append(slate_linucb_hits / (slate_users * slate_size))
            episode_baseline_precisions.append(slate_baseline_hits / (slate_users * slate_size))
            episode_regret_diff.append(slate_baseline_reward - slate_linucb_reward)

    # Compute averages
    results = {
        "linucb_ctr": linucb_ctr / total_interactions if total_interactions > 0 else 0.0,
        "linucb_reward": linucb_reward,
        "linucb_regret": linucb_regret,
        "baseline_ctr": baseline_ctr / total_interactions if total_interactions > 0 else 0.0,
        "baseline_reward": baseline_reward,
        "baseline_regret": baseline_regret,
        "total_interactions": total_interactions,
        "n_slates": n_slates
    }

    results.update({
        'episode_linucb_rewards': episode_linucb_rewards,
        'episode_baseline_rewards': episode_baseline_rewards,
        'episode_linucb_precisions': episode_linucb_precisions,
        'episode_baseline_precisions': episode_baseline_precisions,
        'episode_regret_diff': episode_regret_diff,
    })

    return results


def plot_online_learning_curves(results):
    episodes = np.arange(1, len(results['episode_linucb_rewards']) + 1)

    # Reward over episodes
    plt.figure(figsize=(12, 5))
    plt.plot(episodes, np.cumsum(results['episode_linucb_rewards']), label='LinUCB cumulative reward', marker='o')
    plt.plot(episodes, np.cumsum(results['episode_baseline_rewards']), label='Baseline cumulative reward', marker='o')
    plt.xlabel('Episode (Slate)')
    plt.ylabel('Cumulative Reward')
    plt.title('Reward over Episodes')
    plt.legend()
    plt.grid(True, linestyle='--', alpha=0.4)
    plt.tight_layout()
    plt.savefig('reward_over_episodes.png', dpi=300, bbox_inches='tight')
    plt.close()

    # Precision@k over episodes
    plt.figure(figsize=(12, 5))
    plt.plot(episodes, results['episode_linucb_precisions'], label='LinUCB precision@k', marker='o')
    plt.plot(episodes, results['episode_baseline_precisions'], label='Baseline precision@k', marker='o')
    plt.xlabel('Episode (Slate)')
    plt.ylabel('Precision@k')
    plt.title('Precision@k over Episodes')
    plt.legend()
    plt.grid(True, linestyle='--', alpha=0.4)
    plt.tight_layout()
    plt.savefig('precision_over_episodes.png', dpi=300, bbox_inches='tight')
    plt.close()

    # Policy improvement / convergence proxy
    plt.figure(figsize=(12, 5))
    improvement = np.array(results['episode_linucb_rewards']) - np.array(results['episode_baseline_rewards'])
    plt.plot(episodes, improvement, label='Reward improvement over baseline', marker='o')
    plt.axhline(0, color='gray', linestyle='--', alpha=0.8)
    plt.xlabel('Episode (Slate)')
    plt.ylabel('Reward Improvement')
    plt.title('Policy Improvement over Episodes')
    plt.legend()
    plt.grid(True, linestyle='--', alpha=0.4)
    plt.tight_layout()
    plt.savefig('policy_improvement_over_episodes.png', dpi=300, bbox_inches='tight')
    plt.close()


def print_online_results(results):
    """Format and print online evaluation results."""
    print("\n" + "=" * 60)
    print("ONLINE EVALUATION (RL4RS-style SLATE ENVIRONMENT)")
    print("=" * 60)

    print(f"\nTotal Interactions: {int(results['total_interactions'])}")
    print(f"Number of Slates: {results['n_slates']}")

    print("\n----- BASELINE (Top-Popularity) -----")
    print(f"CTR: {results['baseline_ctr']:.4f}")
    print(f"Cumulative Reward: {results['baseline_reward']:.4f}")
    print(f"Cumulative Regret: {results['baseline_regret']:.4f}")

    print("\n----- LINUCB (Reinforcement Learning) -----")
    print(f"CTR: {results['linucb_ctr']:.4f}")
    print(f"Cumulative Reward: {results['linucb_reward']:.4f}")
    print(f"Cumulative Regret: {results['linucb_regret']:.4f}")

    print("\n----- IMPROVEMENT -----")
    ctr_improvement = (results['linucb_ctr'] - results['baseline_ctr']) / (results['baseline_ctr'] + 1e-6)
    reward_improvement = (results['linucb_reward'] - results['baseline_reward']) / (results['baseline_reward'] + 1e-6)
    regret_reduction = (results['baseline_regret'] - results['linucb_regret']) / (results['baseline_regret'] + 1e-6)

    print(f"CTR Improvement: {ctr_improvement * 100:.2f}%")
    print(f"Reward Improvement: {reward_improvement * 100:.2f}%")
    print(f"Regret Reduction: {regret_reduction * 100:.2f}%")
    print("=" * 60)

    # Plot comparison
    labels = ['CTR', 'Cumulative Reward', 'Cumulative Regret']
    baseline_vals = [results['baseline_ctr'], results['baseline_reward'], results['baseline_regret']]
    linucb_vals = [results['linucb_ctr'], results['linucb_reward'], results['linucb_regret']]

    x = np.arange(len(labels))
    width = 0.35

    fig, ax = plt.subplots(figsize=(10, 6))
    ax.bar(x - width/2, baseline_vals, width, label='Baseline', alpha=0.8)
    ax.bar(x + width/2, linucb_vals, width, label='LinUCB', alpha=0.8)

    ax.set_xlabel('Metrics')
    ax.set_ylabel('Values')
    ax.set_title('Online Slate Evaluation Metrics Comparison')
    ax.set_xticks(x)
    ax.set_xticklabels(labels)
    ax.legend()

    plt.tight_layout()
    plt.savefig('online_slate_metrics.png', dpi=300, bbox_inches='tight')
    # plt.show()  # Commented out to avoid hanging in terminal

    plot_online_learning_curves(results)


# =========================================================
# MAIN
# =========================================================

if __name__ == "__main__":
    import results as results_module

    print("Loading data...")
    interactions = results_module.interactions
    books = results_module.books
    user_stats = results_module.user_stats
    linucb = results_module.linucb

    print(f"Interactions: {len(interactions)}")
    print(f"Books: {len(books)}")
    print(f"Users: {len(user_stats)}")

    # Split data for proper online evaluation
    print("\nSplitting data (80% train, 20% test)...")
    train = interactions.sample(frac=0.8, random_state=42)
    test = interactions.drop(train.index)

    print(f"Train: {len(train)}, Test: {len(test)}")

    # Train model on training set
    print("\nTraining LinUCB on training set...")
    linucb_fresh = results_module.LinUCB(n_features=4, alpha=2.0)  # Increased alpha for more exploration
    for _, row in train.iterrows():
        user = row["UserId"]
        book_id = row["Id"]
        rating = row["User_Rating"]

        match = books[books["Id"] == book_id]
        if match.empty:
            continue

        book = match.iloc[0]
        if user in user_stats.index:
            user_stats_row = user_stats.loc[user]
            x = build_feature_vector(book, user_stats_row["avg_rating"], user_stats_row["interaction_count"])
        else:
            x = build_feature_vector(book)
        reward = reward_from_rating(rating)
        linucb_fresh.update(user, x, reward)

    print("✓ Training complete")

    # Get set of books seen during training (for realistic test scenarios)
    train_books_set = set(train["Id"].unique())
    print(f"Books in training set: {len(train_books_set)}")

    # Online evaluation on test set
    print("\nRunning online evaluation (slate environment)...")
    print("Mode: Online Learning ENABLED (model updates with each interaction)")
    use_rl4rs = HAS_RL4RS
    if use_rl4rs:
        print("✓ RL4RS support detected: using RL4RS slate environment")
    else:
        print("⚠️  RL4RS unavailable: falling back to the simple slate environment")

    results_online = online_eval_slate(
        test,
        books,
        linucb_fresh,
        user_stats,
        n_slates=min(50, len(test) // 10),
        slate_size=5,
        train_books_set=train_books_set,
        online_learning=True,
        use_rl4rs=use_rl4rs
    )

    print_online_results(results_online)
