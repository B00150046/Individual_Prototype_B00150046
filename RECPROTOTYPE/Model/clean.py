from typing import Union

from fastapi import FastAPI

# rec
import numpy as np
import pandas as pd
import sklearn
from scipy.sparse import csr_matrix
import matplotlib.pyplot as plt
import seaborn as sns
from sklearn.neighbors import NearestNeighbors


ratings = pd.read_csv("ratings.csv")
ratings.head()


def create_matrix(df):
    user_mapper = {uid: i for i, uid in enumerate(df['userId'].unique())}
    movie_mapper = {mid: i for i, mid in enumerate(df['movieId'].unique())}
    movie_inv_mapper = {i: mid for mid, i in movie_mapper.items()}

    user_index = df['userId'].map(user_mapper)
    movie_index = df['movieId'].map(movie_mapper)

    X = csr_matrix((df["rating"], (movie_index, user_index)),
                   shape=(len(movie_mapper), len(user_mapper)))
    return X, movie_mapper, movie_inv_mapper






def recommend_similar(movie_title, df, X, movie_mapper, movie_inv_mapper, k=5):
    movie_id = df[df['title'] == movie_title]['movieId'].iloc[0]
    movie_idx = movie_mapper[movie_id]
    movie_vec = X[movie_idx]

    model = NearestNeighbors(metric='cosine', algorithm='brute')
    model.fit(X)
    distances, indices = model.kneighbors(movie_vec, n_neighbors=k + 1)

    neighbor_ids = [movie_inv_mapper[i] for i in indices.flatten()[1:]]
    recommendations = df[df['movieId'].isin(neighbor_ids)]['title'].unique()

    print(f"\nBecause you liked **{movie_title}**, you might also enjoy:")
    
    for rec in recommendations:
        print(f"- {rec}")
    return recommendations

app = FastAPI()












@app.get("/")
def read_root():
    return {"Hello": "World"}


@app.get("/getRec/{item_id}")
def read_item(item_id: int, q: Union[str, None] = None):

    X, movie_mapper, movie_inv_mapper = create_matrix(ratings)

    user_item_matrix = ratings.pivot_table(
        index="title", columns="userId", values="rating")
    print(user_item_matrix.iloc[:10, :5])


    result = recommend_similar("The Dark Knight", ratings, X,
                  movie_mapper, movie_inv_mapper, k=5)

    return {str(result)}
    
    
    
    
    

