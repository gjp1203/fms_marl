import os, os.path
import tensorflow as tf
from tensorflow.python.framework import graph_util
import csv
import numpy as np
import sqlite3
import math
from jinja2.runtime import to_string
#import matplotlib.pyplot as plt

class DeepQNetwork(object):
    def __init__(self, lr, n_actions, name, fc1_dim=20, fc2_dim=20, fc3_dim=20, input_dim=(20,), chkpt_dir='./dqn'):
        self.lr = lr
        self.n_actions = n_actions
        self.name = name
        self.fc1_dim = fc1_dim
        self.fc2_dim = fc2_dim
        self.fc3_dim = fc3_dim
        self.chkpt_dir = chkpt_dir
        self.input_dim = input_dim
        self.sess = tf.Session()
        self.build_network()
        self.sess.run(tf.global_variables_initializer())
        self.saver = tf.train.Saver()
        
        'One file per Run of Simulation'
        NumOfFiles = self.getNumberOfFiles()
        self.checkpoint_file = os.path.join(chkpt_dir,'deepqnet.ckpt_2_' + str(NumOfFiles))
        
        self.params = tf.get_collection(tf.GraphKeys.TRAINABLE_VARIABLES, scope=self.name)

    def build_network(self):
        with tf.variable_scope(self.name):
            self.input = tf.placeholder(tf.float32, shape=[None, *self.input_dim], name="input")
            self.actions = tf.placeholder(tf.float32, shape=[None, self.n_actions], name="action_taken")
            self.q_target = tf.placeholder(tf.float32, shape=[None, self.n_actions], name="q_value")

            dense1 = tf.layers.dense(self.input, units=self.fc1_dim, activation=tf.nn.relu, name="input_layer")
            dense2 = tf.layers.dense(dense1, units=self.fc2_dim, activation=tf.nn.relu,)
            dense3 = tf.layers.dense(dense2, units=self.fc3_dim, activation=tf.nn.relu,)
            self.Q_values = tf.layers.dense(dense3, units=self.n_actions, name="output_layer")

            #self.q = tf.reduce_sum(tf.multiply(self.Q_values, self.actions))

            self.loss = tf.reduce_mean(tf.square(self.Q_values - self.q_target))
            #self.train_op = tf.train.RMSPropOptimizer(self.lr).minimize(self.loss)
            self.train_op = tf.train.AdamOptimizer(self.lr).minimize(self.loss)
        
        

    def load_checkpoint(self):
        print("...Loading checkpoint...")
        self.saver.restore(self.sess, self.checkpoint_file)

    def save_checkpoint(self):
        print("...Saving checkpoint...")
        self.saver.save(self.sess, self.checkpoint_file)

    def save_to_pb(self): # Speichern des Funktionsapproximators für die Integration in die Auftragsagenten
        print("...Saving pb...")

        #for op in tf.get_default_graph().get_nodes():
         #   print(str(op.name))
        
        output_node_names = "q_eval/output_layer/BiasAdd" # Definition der Ausgangschicht

        output_graph_def = graph_util.convert_variables_to_constants(
            self.sess,                              
            tf.get_default_graph().as_graph_def(),      
            output_node_names.split(",")
        )
        model_file = model_file = os.path.abspath(os.path.join(os.path.dirname( __file__ ), 'function_approximator/saved_network_2.pb'))
        with tf.gfile.GFile(model_file, "wb") as f:
            f.write(output_graph_def.SerializeToString())
            
    def getNumberOfFiles(self):
        
        if os.path.split(os.getcwd())[1] == "dqn_agent":
            NameList = os.listdir('.')
            RelevantFiles = [] 
            NumberOfFiles = 0
            for name in NameList:
                if os.path.isfile(name) == True:
                    if name[:3] != "dqn":   
                        if name[:4] != "main":
                            if name[:4] != "all_":
                                RelevantFiles.append(name)
                                NumberOfFiles = len(RelevantFiles) +1 
                                print(NameList)
                                print(RelevantFiles)
                                print("#: " + str(NumberOfFiles))
            return NumberOfFiles
                          
        elif os.path.split(os.getcwd())[1] == "multiagent":
            NameList = os.listdir(os.chdir(os.getcwd()+'\dqn_agent'))
            RelevantFiles = [] 
            NumberOfFiles = 0
            for name in NameList:
                if os.path.isfile(name) == True:
                    if name[:3] != "dqn":   
                        if name[:4] != "main":
                            if name[:4] != "all_":
                                RelevantFiles.append(name)
                                NumberOfFiles = len(RelevantFiles) +1
                                print(RelevantFiles)
                                print(NameList)
                                print("#: " + str(NumberOfFiles))
            return NumberOfFiles

class Agent(object):
    def __init__(self, alpha, gamma, mem_size, n_actions, batch_size, replace_target=10, input_dim=(11,),
                 q_next_dir=os.path.abspath(os.path.join(os.path.dirname( __file__ ), 'next')),            #'C:/Users/Fohlmeister/eclipse-workspace/MAS_RL/dqn_agent/next'
                q_eval_dir=os.path.abspath(os.path.join(os.path.dirname( __file__ ), 'eval')),): 
        
        #tf.reset_default_graph()
        self.action_space = [i for i in range(n_actions)]
        self.n_actions = n_actions
        self.gamma = gamma
        self.mem_size = mem_size
        self.mem_cntr = 0
        self.batch_size = batch_size
        self.replace_target = replace_target
        self.q_next = DeepQNetwork(alpha, n_actions, input_dim=input_dim, name='q_next', chkpt_dir=q_next_dir)
        self.q_eval = DeepQNetwork(alpha, n_actions, input_dim=input_dim, name='q_eval', chkpt_dir=q_eval_dir)
        self.state_memory = np.zeros((self.mem_size, *input_dim))
        self.new_state_memory = np.zeros((self.mem_size, *input_dim))
        self.action_memory = np.zeros((self.mem_size, self.n_actions), dtype=np.int8)
        self.reward_memory = np.zeros(self.mem_size)
        self.terminal_memory = np.zeros(self.mem_size, dtype=np.int8)
        self.loss = []
        
        
        # short initialization for a log file
        # writes down the input parameters and the header for the columns
        NumberOfDocumentedFiles = self.getNumberOfDocumentedFiles()
        if NumberOfDocumentedFiles == None: 
            NumberOfDocumentedFiles = 1
         
        if os.path.split(os.getcwd())[1] == "multiagent":
            self.logname = 'losslog_2_' + str(NumberOfDocumentedFiles) +'.csv'
            with open(os.chdir(os.getcwd()+'\dqn_agent') + self.logname, 'w+', newline='') as losslogfile:
                writer = csv.writer(losslogfile, delimiter = ';')
                input_parameters = ['gamma=' +str(gamma), 'alpha=' +str(alpha), 'dimension=' +str(input_dim[0]), 'mem_size=' +str(mem_size), 'batch_size=' +str(batch_size), 'n_actions' +str(n_actions)]
                writer.writerow(input_parameters)
                logheader = []
                logheader.append(str('loss'))
                for i in range(input_dim[0]):
                    logheader.append('q_eval[0' + str(i) +']')
                for j in range(n_actions):
                    logheader.append('state_batch[0' + str(j) + ']')
                writer.writerow(logheader) 
        else:
            self.logname = 'losslog_2_' + str(NumberOfDocumentedFiles) +'.csv'
            with open(self.logname, 'w+', newline='') as losslogfile:
                writer = csv.writer(losslogfile, delimiter = ';')
                input_parameters = ['gamma=' +str(gamma), 'alpha=' +str(alpha), 'dimension=' +str(input_dim[0]), 'mem_size=' +str(mem_size), 'batch_size=' +str(batch_size), 'n_actions' +str(n_actions)]
                writer.writerow(input_parameters)
                logheader = []
                logheader.append(str('loss'))
                for i in range(input_dim[0]):
                    logheader.append('q_eval[0' + str(i) +']')
                for j in range(n_actions):
                    logheader.append('state_batch[0' + str(j) + ']')
                writer.writerow(logheader) 
       
    def fetch(self): 
        connection = sqlite3.connect(os.path.abspath(os.path.join(os.path.dirname( __file__ ),\
                                     '..', 'replay_memory/Replay_Memory_global.db')))

        cursor = connection.cursor()
        cursor.execute("SELECT Count (ID) from Memories")
        result = cursor.fetchone() [0]

        max_mem = result if result < self.mem_size else self.mem_size

        # Fetch the number of samples specified by batch_size, or if 
        # insufficient get everything. 
        cursor.execute("SELECT * FROM Memories ORDER BY RANDOM() LIMIT ?",\
                      (min(self.batch_size, max_mem), ))
        result = cursor.fetchall()
       
        # Store everything lists: 
        state_batch = []
        new_state_batch = []
        action_indices = []
        reward_batch = []
        terminal_batch = []
        for row in result:
            state_batch.append(np.array(row[1].split(","), dtype=np.float32))
            new_state_batch.append(np.array(row[2].split(","), dtype=np.float32))
            action_indices.append(row[3]) # action indecies
            reward_batch.append(row[4])
            terminal_batch.append(row[5])
  
        # Get one hot encoding for the actions
        action_batch = np.zeros((len(action_indices), self.n_actions))
        action_batch[np.arange(len(action_indices)), action_indices] = 1
        return state_batch, new_state_batch, action_indices, action_batch, reward_batch, terminal_batch

    def learn(self, i):
        if i % self.replace_target == 0:
            self.update_graph()

        state_batch, new_state_batch, action_indices, action_batch, reward_batch, terminal_batch = self.fetch()  
        q_eval = self.q_eval.sess.run(self.q_eval.Q_values, feed_dict={self.q_eval.input: state_batch})
        q_next = self.q_next.sess.run(self.q_next.Q_values, feed_dict={self.q_next.input: new_state_batch})
        idx = np.arange(q_eval.shape[0])
        q_target = q_eval.copy()
        q_target[idx, action_indices] = reward_batch + self.gamma*np.max(q_next, axis=1 )*terminal_batch
        '''
        #old function to calculate and display loss graph directly
        self.loss.append(math.pow(q_eval[idx, action_indices] - q_target[idx, action_indices], 2))
        plt.plot(self.loss)
        if i % 2000 == 0:
            plt.show()
        '''

        _, losspoint = self.q_eval.sess.run([self.q_eval.train_op, self.q_eval.loss],
                        feed_dict={self.q_eval.input: state_batch,
                                   self.q_eval.actions: action_batch,
                                   self.q_eval.q_target: q_target})
        #losspoint = math.pow(q_eval[idx, action_indices] - q_target[idx, action_indices], 2) #loss Berechnung 
       
        '''
        #old logging part for variable dimensions
        f = open('losslog_old.csv','a')    

        
        logline = str(losspoint) + ';' #line to write to logging file
        for i in range(len(q_eval[0])):
            logline += str(q_eval[0,i]) + ';'
        for i in range(len(state_batch[0])):
            logline += str(state_batch[0,i]) + ';'
        f.write(logline + '\n')
        
        #even older logging part for hardcoded dimension = 3
        f.write(str(losspoint)+';'+ str(q_eval[0,0])+';'+ str(q_eval[0,1])+';'+ str(q_eval[0,2])+';'+str(state_batch[0,0])+';'+str(state_batch[0,1])+';'+str(state_batch[0,2])+';'+'\n')

        f.close()
        '''
        #write loss, q_eval and state_batch to a logfile
        with open(self.logname, 'a', newline='') as losslogfile:
            logline = []
            print("Losspoint: " + to_string(losspoint) + " on batch size: " + to_string(self.batch_size))
            logline.append(losspoint)
            for k in range(len(q_eval[0])):
                logline.append(q_eval[0,k])
            #for l in range(len(state_batch[0])):
            #    logline.append(state_batch[0,l])
            #for state in state_batch:
                #doc_state = np.split(state,1)
                #for l in range (len(doc_state)):
                    #logline.append(doc_state[l])
                #logline.append(state)
            #for l in range (len(state_batch[0])):
                #logline.append(state_batch[0,l])
            writer = csv.writer(losslogfile, delimiter = ';')
            writer.writerow(logline) 
        
    def save_models(self):
        self.q_eval.save_to_pb()
        self.q_next.save_checkpoint()
        self.q_eval.save_checkpoint()

    def load_models(self):
        self.q_eval.load_checkpoint()
        self.q_next.load_checkpoint()

    def update_graph(self):
        t_params = self.q_next.params
        e_params = self.q_eval.params
        for t, e in zip(t_params, e_params):
            self.q_eval.sess.run(tf.assign(t,e))
            
    def getNumberOfDocumentedFiles(self):
        NumberOfDocumentedFiles = 0
        if os.path.split(os.getcwd())[1] == "dqn_agent":
            NameList = os.listdir('.')
            RelevantFiles = [] 
            
            for name in NameList:
                if os.path.isfile(name) == True:
                    if name[:3] != "dqn":   
                        if name[:4] != "main":
                            if name[:4] != "all_":
                                RelevantFiles.append(name)
                                NumberOfDocumentedFiles = len(RelevantFiles) +1 
                                print(NameList)
                                print(RelevantFiles)
                                print("#: " + str(NumberOfDocumentedFiles))
            return NumberOfDocumentedFiles
                          
        elif os.path.split(os.getcwd())[1] == "multiagent":
            NameList = os.listdir(os.chdir(os.getcwd()+'\dqn_agent'))
            RelevantFiles = [] 
            
            for name in NameList:
                if os.path.isfile(name) == True:
                    if name[:3] != "dqn":   
                        if name[:4] != "main":
                            if name[:4] != "all_":
                                RelevantFiles.append(name)
                                NumberOfDocumentedFiles = len(RelevantFiles) +1
                                print(RelevantFiles)
                                print(NameList)
                                print("#: " + str(NumberOfDocumentedFiles))
            return NumberOfDocumentedFiles
        
