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
from keras.models import Sequential
from keras.layers import Dense
import warnings
warnings.filterwarnings("ignore")


def create_model():
    model = Sequential()
    model.add(Dense(60, input_dim=35, activation='relu'))
    model.add(Dense(20, activation='relu'))
    model.add(Dense(35, activation='softmax'))
    # compile model
    model.compile(loss='categorical_crossentropy', optimizer='adam', metrics=['accuracy'])
    return model

# train model
def train_model(model, data):
    # X = np.stack(data[['word2vec']].to_numpy().reshape(-1))
    # y = np.stack(data[['LABEL']].to_numpy().reshape(-1))

    X = data.loc[:, "col_1":"col_35"]
    y = data["LABEL"]

    # X = np.stack(X.to_numpy().reshape(-1))
    # y = np.stack(y.to_numpy().reshape(-1))


    X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.1, shuffle=False)


    y_train = keras.utils.to_categorical(y_train, 35)
    y_test = keras.utils.to_categorical(y_test, 35)

    train_start_time = time.time()
    print("Training started at: ", str(datetime.datetime.now()))
    model.fit(X_train, y_train, epochs=100,  batch_size=100, validation_split=0.1, verbose=2)
    print("Training completed at: ", str(datetime.datetime.now()))
    train_end_time = time.time()
    print("Time taken to train: ", (train_end_time-train_start_time))
    
    print("\nModel ealuation on Test set:")
    # print("X_test.shape: ", X_test.shape)
    model.evaluate(x=X_test, y=y_test)
    return model

if __name__ == "__main__":
    # print("\nTraining for "+str(sys.argv[1])+ " begins...")
    # path = "/Users/taehwan/Desktop/Research/pastry_amazon/src/rice/tutorial/FederatedML_Amazon"
    path = "/home/chirag/fl/MLP/FederatedML_speech"
    # load the model
    # init_model = keras.models.load_model(path+'/init_model_'+str(sys.argv[1])+'_'+str(sys.argv[2])+'.h5')

    init_model = create_model()

    print("1. Init Model Loaded")

    # load data
    data = pd.read_csv(path+"/data_test.csv")
    
    new_model = train_model(init_model, data)
    
    print("6. Model Trained")
    
    # new_model.save(path+'/new_model_'+str(sys.argv[1])+'_'+str(sys.argv[2])+'.h5')
    
    print("7. Model Saved")
    
    # print("Training for "+str(sys.argv[1])+ " ends...")