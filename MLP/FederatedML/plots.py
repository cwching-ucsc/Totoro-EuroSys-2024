from re import S
import matplotlib.pyplot as plt
import datetime
import time, os

class Plot:
    def __init__(self) -> None:
        self.start_time = time.time()
        self.x_values = [0.0]
        self.y_values = [0.0]

    # def set_init_time(self):
    #     self.start_time = time.time()
    
    def add_data_point(self, y_value):
        x_value = time.time() - self.start_time
        self.x_values.append(x_value)
        self.y_values.append(y_value)

    def plot(self, x_values=None, y_values=None, path=None, plot_name='', x_label=None, y_label=None):
        if path is None:
            dirname = os.path.dirname(__file__)
            path = dirname + "/figures/"
        
        if x_values is None:
            x_values = self.x_values
        if y_values is None:
            y_values = self.y_values

        if x_label is not None:
            plt.xlabel(x_label)
        if y_label is not None:
            plt.ylabel(y_label)
        print(x_values, y_values)

        # for i, acc in enumerate(y_values):
        #     if i==0:
        #         plt.annotate("Start", (x_values[i], y_values[i]))
        #     else:
        #         plt.annotate("Round: " + str(i), (x_values[i], y_values[i]))

        plt.plot(x_values, y_values, marker='o')
        # plt.show()
        path += plot_name + '_' + str(datetime.datetime.now()) + ".png"
        plt.savefig(path)
