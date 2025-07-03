parallel-scp -t 0 -h scale.hosts -p 50 -x "-o StrictHostKeyChecking=no -i /home/chirag/.ssh/id_rsa" /home/chirag/fl/failurerecovery/failurerecovery3/failurerecovery3.jar  /home/chirag/FailureRecovery/
parallel-scp -t 0 -h scale.hosts -p 50 -x "-o StrictHostKeyChecking=no -i /home/chirag/.ssh/id_rsa" /home/chirag/fl/failurerecovery/failurerecovery3/user.params  /home/chirag/FailureRecovery/
