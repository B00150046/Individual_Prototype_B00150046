# Lin-UCB Recommender System - Evaluation Guide

## Research Question
**To what extent is a reinforcement-based Recommender System that continuously adapts to user preferences to improve book recommendations effective?**

This evaluation compares a **Lin-UCB** (Reinforcement Learning) model against a **baseline** recommender across both offline and online settings.

---

## Models

### Baseline (Content-based / Popularity-based)
- **Type**: Static heuristic
- **Recommendation**: Top-K books by popularity (rating_count) within candidate set
- **Learning**: None; uses pre-computed book statistics only
- **Advantages**: Fast, stable, no cold-start for items
- **Disadvantages**: Ignores user preferences, no personalization

### Lin-UCB (Reinforcement Learning)
- **Type**: Contextual Multi-Armed Bandit
- **Features**:
  - Book features: avg_rating, log(rating_count)
  - User features: avg_rating, log(interaction_count)
- **Learning**: Updates confidence intervals based on user feedback (ratings)
- **Exploration-Exploitation**: Balances exploration via confidence bounds (α=1.0)
- **Advantages**: Personalized, adapts to user feedback, reduces regret over time
- **Disadvantages**: Requires initial training data, exploration cost early on

---

## Evaluation Methodology

### 1. Offline Evaluation (Batch)

**Setup**:
- Train/test split: 80% training, 20% test interactions
- Candidate pool: 100 items per recommendation (99 random + 1 true item)
- Metrics: Precision@5, Recall@5, F1@5, NDCG@5, MAP@5

**Why it matters**:
- Measures model quality on held-out data
- No online experimental cost
- Shows how well each model learned from training data

**Example Results** (1% dataset sample):
```
===== OFFLINE METRICS =====

BASELINE (Top-Popularity):
  Precision@K: 0.0%
  Recall@K: 0.0%
  F1@K: 0.0%
  NDCG@K: 0.0%
  MAP@K: 0.0%

LIN-UCB (RL):
  Precision@K: 0.51%
  Recall@K: 2.56%
  F1@K: 0.85%
  NDCG@K: 2.14%
  MAP@K: 1.99%

→ Lin-UCB outperforms baseline offline
```

**Key Finding**: Lin-UCB learns user-specific preferences better than a static popularity baseline.

---

### 2. Online Evaluation (Simulated Environment - RL4RS-style)

**Setup**:
- Sequential interactions in slate environment
- Candidates: Items from training set + true item
- Online learning: Model updates after each recommendation (continuous adaptation)
- Metrics: CTR, Cumulative Reward, Cumulative Regret, Regret Reduction

**Why it matters**:
- Simulates real deployment scenario
- Tests continuous learning capability
- Measures long-term effectiveness (cumulative reward, regret)
- Reflects RL's strength in sequential decision-making

**Evaluation Flow**:
```
For each slate (batch of users):
  1. User comes with true item context
  2. Recommend top-5 items to user (both models)
  3. Observe if user clicks (hit/miss + rating)
  4. LIN-UCB: Update model with feedback
  5. BASELINE: No learning
  6. Accumulate: CTR, Reward, Regret
```

**Metrics Explained**:
- **CTR (Click-Through Rate)**: % of times recommended item matched user preference
- **Cumulative Reward**: Sum of ratings for correctly recommended items
- **Cumulative Regret**: Sum of missed opportunities (optimal value - actual)
- **Regret Reduction**: How much better RL reduces regret vs baseline

---

## Running Evaluations

### Offline Evaluation (Batch)
```bash
cd /path/to/reinforcement_based
python3 results.py
```

**Output**:
- Offline metrics (Precision, Recall, F1, NDCG, MAP)
- Online session-level metrics (CTR, Reward, Regret)

### Online Evaluation (RL4RS-style Simulation)
```bash
python3 online_eval_rl4rs.py
```

**Output**:
- Slate-based sequential evaluation
- Compares baseline vs Lin-UCB with continuous learning
- Shows regret curves and cumulative rewards

---

## Key Insights

### Lin-UCB Strengths:
1. **Personalization**: Adapts to individual user preferences
2. **Progressive Learning**: Improves over time with more interactions
3. **Regret Minimization**: Uses optimism-under-uncertainty to balance exploration/exploitation
4. **Contextual Decisions**: Incorporates both item and user features

### Baseline Strengths:
1. **Simplicity**: Easy to implement and maintain
2. **Stability**: Consistent performance, no variance from exploration
3. **Cold-start Handling**: Works immediately for new items

### When Lin-UCB Wins:
- User preferences are consistent and learnable
- Enough data to train meaningful user models
- Long-term engagement matters (cumulative reward)
- Personalization adds value over generic popularity

### When Baseline Wins:
- Very cold-start environments (few ratings)
- Users are highly unpredictable
- Exploration cost is too high
- Computational simplicity is critical

---

## Future Enhancements

1. **Hybrid Approach**: Combine baseline popularity with Lin-UCB for cold-start users
2. **Content Features**: Add book metadata (genre, author, publication year)
3. **Multi-Armed Bandits**: Use Thompson Sampling or Upper Confidence Bound variants
4. **Deep Learning Features**: Learn embeddings instead of hand-crafted features
5. **Real RL4RS Integration**: Deploy on actual RL4RS dataset with slate environment
6. **A/B Testing**: Run true online experiment in production system

---

## References

- **Lin-UCB Paper**: Li et al., "A Contextual-Bandit Approach to Personalized News Recommendation" (2010)
- **RL4RS**: https://github.com/fuxiAIlab/RL4RS
- **Amazon Books**: RecSysDatasets collection

