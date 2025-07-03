#!/bin/bash
# LOG_LOCATION="/Users/taehwan/Desktop/Research/pastry_amazon/src/rice/tutorial/FederatedML_Amazon/LOGS"
# LOG_LOCATION="/home/ec2-user/FederatedML/LOGS"
LOG_LOCATION="/home/chirag/fl/MLP/FederatedML/LOGS"

exec >> $LOG_LOCATION/init_job_log.txt 2>&1
# source "/Users/taehwan/Desktop/Research/pastry_amazon/src/rice/tutorial/FederatedML_Amazon/headnode/bin/activate"
# python /Users/taehwan/Desktop/Research/pastry_amazon/src/rice/tutorial/FederatedML_Amazon/initialmodel.py

# source "/home/ec2-user/FederatedML/headnode/bin/activate"
# python /home/ec2-user/FederatedML/initialmodel.py

source "/home/chirag/fl/MLP/FederatedML/headnode/bin/activate"
python /home/chirag/fl/MLP/FederatedML/initialmodel.py