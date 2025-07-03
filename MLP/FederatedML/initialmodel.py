import os
os.environ['TF_CPP_MIN_LOG_LEVEL'] = '3' 
from keras.models import Sequential
from keras.layers import Dense
import warnings
warnings.filterwarnings("ignore")
import sys

def create_model(path):
    model = Sequential()
    model.add(Dense(60, input_dim=50, activation='relu'))
    model.add(Dense(20, activation='relu'))
    model.add(Dense(1, activation='sigmoid'))
    # compile model
    model.compile(loss='binary_crossentropy', optimizer='adam', metrics=['accuracy'])
    model.save(path+'/init_model_0.h5')


if __name__ == '__main__':
    # path = '/Users/taehwan/Desktop/Research/pastry_amazon/src/rice/tutorial/FederatedML_Amazon'
    path = "/home/chirag/fl/MLP/FederatedML"
    # path = "/home/ec2-user/FederatedML"
    print("Initial model building starts...")
    create_model(path)
    print("Initial model building ends...")