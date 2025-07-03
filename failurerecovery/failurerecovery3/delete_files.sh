parallel-ssh -t 0 -h scale.hosts -p 100 -x "-o StrictHostKeyChecking=no -i /home/chirag/.ssh/id_rsa" -i "cd FailureRecovery && rm user.params.new"
