#!/usr/bin/python
import os
os.environ['TF_CPP_MIN_LOG_LEVEL'] = '3' 
import datetime, time
from os import walk
from tensorflow import keras
from numpy import array, average
from keras.models import clone_model
from plots import Plot
import numpy as np
import pandas as pd
from tensorflow import keras
import texthero as hero
from gensim.models import KeyedVectors
from train import vectorize
from sklearn.model_selection import train_test_split
import pickle
import warnings
import sys
warnings.filterwarnings("ignore")


def save_plot_object(plot_object):
	dirname = os.path.dirname(__file__)
	with open(dirname + '/figures/pickle_files/combine_models.pickle', 'wb') as handle:
			pickle.dump(plot_object, handle, protocol=pickle.HIGHEST_PROTOCOL)

def load_plot_object():
	dirname = os.path.dirname(__file__)
	if not os.listdir(dirname + '/figures/pickle_files/'):
		combine_models_plot = Plot()
		save_plot_object(combine_models_plot)
	
	combine_models_plot = None
	with open(dirname + '/figures/pickle_files/combine_models.pickle', 'rb') as handle:
		combine_models_plot = pickle.load(handle)
	return combine_models_plot

# load all models from folder
def load_all_models(path):
	mypath = path + "/Received_Models"
	filenames = next(walk(mypath), (None, None, []))[2]  # [] if no file
	all_models = []
	for file in filenames:
		if file[0] != ".":
			model = keras.models.load_model(mypath+"/"+file)
			all_models.append(model)
	return all_models

def combine_models(members, weights):
    # how many layers need to be averaged
    n_layers = len(members[0].get_weights())
    # create a set of average model weights
    avg_model_weights = list()
    for layer in range(n_layers):
        # collect this layer from each model
        layer_weights = array([model.get_weights()[layer] for model in members])
        # weighted average of weights for this layer
        avg_layer_weights = average(layer_weights, axis=0, weights=weights)
        # store average layer weights
        avg_model_weights.append(avg_layer_weights)
        
    # create a new model with the same structure
    model = clone_model(members[0])
    model.set_weights(avg_model_weights)
    model.compile(loss='binary_crossentropy', optimizer='adam', metrics=['accuracy'])
    return model


def test_combined_model(combined_model, combined_models_plot):
	path = "/home/chirag/fl/MLP/FederatedML_speech"
    
	data = pd.read_csv(path+"/data_test.csv")
	model = KeyedVectors.load_word2vec_format(path+"/glove-50")
	data['clean_content'] = hero.clean(data['CONTENT'])
	data['clean_content'] = data['clean_content'].str.split(' ')
	data['word2vec'] = data['clean_content'].apply(lambda x: vectorize(x, model))
	X_test = np.stack(data[['word2vec']].to_numpy().reshape(-1))
	y_test = np.stack(data[['LABEL']].to_numpy().reshape(-1))

	# X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.25, shuffle=False)

	loss, accuracy = combined_model.evaluate(x=X_test, y=y_test)
	loss = round(loss, 2)
	accuracy = round(accuracy*100, 2)
	print("Combined Model Loss: ", loss)
	print("Combined Model Accuracy: ", accuracy)
	combined_models_plot.add_data_point(accuracy)
	combined_models_plot.plot(plot_name="Combined_models_plot", x_label="Time (s)", y_label="Accuracy")
	

if __name__ == '__main__':
	combined_models_plot = load_plot_object()
	print("\nModel Combining begins...")
	# path = "/Users/taehwan/Desktop/Research/pastry_amazon/src/rice/tutorial/FederatedML_Amazon"
	# path = "/home/ec2-user/FederatedML"
	path = "/home/chirag/fl/MLP/FederatedML"
	all_models = load_all_models(path)
	# prepare an array of equal weights
	n_models = len(all_models)
	weights = [1/n_models for i in range(1, n_models+1)]

	# combine models
	combine_start_time = time.time()
	print("Combining started at: ", str(datetime.datetime.now()))
	combined_model = combine_models(all_models, weights)
	print("Combining completed at: ", str(datetime.datetime.now()))
	combine_end_time = time.time()
	print("Time taken to combine models: ", (combine_end_time-combine_start_time))

	test_combined_model(combined_model=combined_model, combined_models_plot=combined_models_plot)

	# save model
	combined_model.save(path + '/init_model_'+str(sys.argv[1])+'.h5')
	print("Model Combining successfully completes...")
	save_plot_object(combined_models_plot)


	