import os
import dqn_agent_4 as dqn
import numpy as np
import tensorflow as tf
from tensorflow.python.platform import gfile

if __name__ == '__main__':
        os.environ["CUDA_DEVICE_ORDER"] = "PCI_BUS_ID"
        os.environ["CUDA_VISIBLE_DEVICES"] = "0"
        os.environ['TF_FORCE_GPU_ALLOW_GROWTH'] = 'true'

        learning_steps = 25000
        load_checkpoint = False

        #agent_Maschine = dqn_maschine.Agent (gamma=0.25, alpha=0.001, input_dim=(100,), n_actions=100, mem_size=10000, batch_size=1)
        
        agent = dqn.Agent(gamma=0.95, alpha=0.001, input_dim=(65,), n_actions=6, mem_size=3000, batch_size=50)
        

        if load_checkpoint:
            agent.load_models()

        '''
        #old version, now handled in agent initialization and for different dimensions
        #create a log file and close it for use in dqn_agent
        f=open('losslog_old.csv', 'w+')
        f.write('loss ; q_eval[00] ; q_eval[01] ; q_eval[02] ; state_batch[00] ; state_batch[01] ; state_batch[02] \n')
        f.close()
        '''

        for i in range(learning_steps):
            #agent_Maschine.learn(i)
            agent.learn(i)
            print(i)
            if i % 10 == 0 and i > 0:
                agent.save_models()
                #agent_Maschine.save_models()
#write all tensors to a file
all_tensors = [tensor for op in tf.get_default_graph().get_operations() for tensor in op.values()]
with open('all_tensors_order.txt', 'w+') as f:
    for item in all_tensors:
        f.write("%s\n" % item)
    f.close()
        
