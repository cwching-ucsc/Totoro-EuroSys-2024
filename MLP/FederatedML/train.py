#!/usr/bin/python
import os
os.environ['TF_CPP_MIN_LOG_LEVEL'] = '3' 
import datetime, time
import numpy as np
import pandas as pd
import sys
from sklearn.model_selection import train_test_split
import texthero as hero
from tensorflow import keras
from gensim.models import KeyedVectors
import warnings
warnings.filterwarnings("ignore")

# vectorize sentences embeddings
def vectorize(sentence, model, dimension=50):
    ans = [0]*dimension
    for word in sentence:
        if word in model:
            ans += model[word]
    return ans

# train model
def train_model(model, data):
    X = np.stack(data[['word2vec']].to_numpy().reshape(-1))
    y = np.stack(data[['LABEL']].to_numpy().reshape(-1))
    # print("X.shape: ", X.shape)
    # X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.25, random_state=42)
    X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.1, shuffle=False)
    
    train_start_time = time.time()
    print("Training started at: ", str(datetime.datetime.now()))
    model.fit(X_train, y_train, epochs=50,  batch_size=400, validation_split=0.3, verbose=2)
    print("Training completed at: ", str(datetime.datetime.now()))
    train_end_time = time.time()
    print("Time taken to train: ", (train_end_time-train_start_time))
    
    print("\nModel ealuation on Test set:")
    # print("X_test.shape: ", X_test.shape)
    model.evaluate(x=X_test, y=y_test)
    return model

if __name__ == "__main__":
    print("\nTraining for "+str(sys.argv[1])+ " begins...")
    # path = "/Users/taehwan/Desktop/Research/pastry_amazon/src/rice/tutorial/FederatedML_Amazon"
    path = "/home/chirag/fl/MLP/FederatedML"
    # load the model
    init_model = keras.models.load_model(path+'/init_model_'+str(sys.argv[1])+'_'+str(sys.argv[2])+'.h5')

    print("1. Init Model Loaded")

    # load data
    data = pd.read_csv(path+"/data_train.csv")
    
    print("2. Data.csv read")
    
    model = KeyedVectors.load_word2vec_format(path+"/glove-50")
    
    print("3. Glove50 Loaded")
    
    # clean text
    data['clean_content'] = hero.clean(data['CONTENT'])
    data['clean_content'] = data['clean_content'].str.split(' ')

    print("4. Data Cleaning Done")

    # vectorize using pre trained word2vec
    data['word2vec'] = data['clean_content'].apply(lambda x: vectorize(x, model))
    
    print("5. Vectorization Done")
    
    new_model = train_model(init_model, data)
    
    print("6. Model Trained")
    
    new_model.save(path+'/new_model_'+str(sys.argv[1])+'_'+str(sys.argv[2])+'.h5')
    
    print("7. Model Saved")
    
    print("Training for "+str(sys.argv[1])+ " ends...")
