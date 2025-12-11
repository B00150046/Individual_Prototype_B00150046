from typing import Union
from fastapi import FastAPI, HTTPException

# rec
import numpy as np
import pandas as pd
import csv
import sklearn
from scipy.sparse import csr_matrix
from sklearn.preprocessing import normalize
import matplotlib.pyplot as plt
import seaborn as sns
from sklearn.neighbors import NearestNeighbors

# ---------- load data once ----------
ratings = pd.read_csv("book1-100.csv")
ratings.dropna(inplace=True)


# ---------- helper to build matrix (same name) ----------
def create_matrix(df):
    user_mapper = {uid: i for i, uid in enumerate(df['UserId'].unique())}
    movie_mapper = {mid: i for i, mid in enumerate(df['Id'].unique())}
    movie_inv_mapper = {i: mid for mid, i in movie_mapper.items()}

    user_index = df['UserId'].map(user_mapper)
    movie_index = df['Id'].map(movie_mapper)

    # build CSR as in your original code; cast ratings to float32 for memory
    X = csr_matrix((df["User_Rating"].astype(np.float32), (movie_index, user_index)),
                   shape=(len(movie_mapper), len(user_mapper)),
                   dtype=np.float32)

    # normalize rows for cosine similarity (so cosine == dot product)
    X = normalize(X, norm='l2', axis=1, copy=False)
    return X, movie_mapper, movie_inv_mapper

# build matrix once and keep mappers
X_global, movie_mapper, movie_inv_mapper = create_matrix(ratings)

# Build quick map from Name -> Id for fast lookup (if multiple same Name, first wins)
name_to_id = ratings.drop_duplicates(subset=['Id']).set_index('Name')['Id'].to_dict()

# ---------- fit NN once at startup ----------
nn_model = NearestNeighbors(metric='cosine', algorithm='brute', n_jobs=-1)
nn_model.fit(X_global)

# ---------- recommend_similar (kept signature, but uses prefit model) ----------
def recommend_similar(book_title, df, X, movie_mapper, movie_inv_mapper, k=5):
    # Defensive: normalize title and check existence
    book_title_clean = book_title.strip()
    if book_title_clean not in name_to_id:
        raise ValueError(f"Book title not found: '{book_title_clean}'")

    # Find book id and index
    book_id = name_to_id[book_title_clean]
    if book_id not in movie_mapper:
        raise ValueError(f"Book id for title not indexed: {book_id}")

    book_idx = movie_mapper[book_id]
    book_vec = X[book_idx]

    # Use pre-fit NN model for performance
    distances, indices = nn_model.kneighbors(book_vec, n_neighbors=k + 1)

    # Convert indices → book IDs
    neighbor_ids = [movie_inv_mapper[i] for i in indices.flatten()]

    # Explicitly remove the same book and keep first k
    neighbor_ids = [nid for nid in neighbor_ids if nid != book_id][:k]

    # Get book names preserving order
    recommendations = []
    for nid in neighbor_ids:
        name_series = df[df['Id'] == nid]['Name']
        if not name_series.empty:
            recommendations.append(name_series.iloc[0])

    print(f"\nBecause you liked **{book_title_clean}**, you might also enjoy:")
    for rec in recommendations:
        print(f"- {rec}")

    return recommendations



# ---------- FastAPI ----------
app = FastAPI()

@app.get("/")
def read_root():
    return {"Hello": "World"}

@app.get("/getAllBooks")
def get_all_books():
    """
    Returns all unique book names from the Name column as JSON.
    """
    # Get all unique book names from the Name column
    all_books = ratings['Name'].unique().tolist()
    
    return {"books": all_books}

@app.get("/getRec/{film_name}")
def read_item(film_name: str, q: Union[str, None] = None):
    # decode URL-encoded spaces and trim
    film_name_clean = film_name.replace("%20", " ").strip()
    print("Finding films similar to " + str(film_name_clean))

    # very small/memory-free operation now (no rebuilding/fitting)
    try:
        recs = recommend_similar(film_name_clean, ratings, X_global,
                                 movie_mapper, movie_inv_mapper, k=5)
    except ValueError as e:
        raise HTTPException(status_code=404, detail=str(e))

    # Return nested structure expected by the Android client
    return {"recommendations": {"recommendations": list(recs)}}


