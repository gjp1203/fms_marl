### READ ME File ################################################
#Authors: 														#
#	Daniel Kemp -- Kemp@ifw.uni-hannover.de --					#
#	Gregory Palmer -- gpalmer@l3s.de --							#
#	Silas Fohlmeister -- Fohlmeister@ifw.uni-hannover.de -- 	#
#																#
#	Last Update: 04/29/2021										#
#################################################################

############
Introduction:
############

This github repo includes the source code and the Simulation environments that the authors used in 
their publication at 2021 ASIM conference for "Simulation in Production and Logistics" with 
the title "Scalable, cooperative multi-agent-reinforcement-learning for order-controlled on-
schedule production in flexible manufacturing systems". 

This framework allows to use a cooperative MARL approach(presented first in https://doi.org/10.1016/j.cirp.2020.04.005) 
for order-scheduling in the professional discrete simulation software Tecnomatix Plant Simulation.

Our goal is to provide other researches, especially the manufacturing community, a low-effort opportunity to make use of this framework.

Legal Notice: We do not claim to provide a source code free from errors. 
However, we cleaned and structured our source code to the best of our knowledge to make it work from the beginning onwards. 

############
Software to be installed:
############

(1)		A Java-based IDE ( We used the latest version of eclipse including PyDev and EGit installed) 
(2)		Python 3.6 (including Tensorflow 1.15)
(3) 	Tecnomatix Plant Simulation (Version 8.2 or higher -- Relevant Methods are written in SimTalk 1. 
		Higher versions that use SimTalk2 might need some adaptions in the code) 
		
		Very Important: A Plant Simulation License with Socket-Interface-support must be used. 
		
############
How to start the Framework:
############
(1) Import or clone this github repo into your IDE. Make sure that all external jar-files in the respective folder 
are included into your dependencies. 

Configure your run-configurations as follows in Eclipse IDE: 
Main Class: 		Jade.boot
Program arguments: -gui Socket_in:supervisor_agents.Socket_in;Socket_out:supervisor_agents.Socket_out;Initialization_agent:supervisor_agents.Initialization_agent
VM arguments:		-Djava.library.path=./jni

If you click on run the JADE-based Multi-Agent-Framework will start and a GUI will show up with all relevant agents. 

(2) Install Plant Simulation and load whatever file from the folder "simulation_model". If this starts without any error message, you are good to go. 

############
Important Classes:
############
(1) In the "src" folder: order_agents is a generic order agent class, which is used to dynamically create software agents for orders created in Plant Simulation
(2) In the "src" folder: machine_agents -> Each java file represents a Machine with a specific skill. In case skills are added, you will need to duplicate the java file
	rename it to the specific skill, e.g. "Cutting" -> Cutting_agent.java. Furthermore, you need to rename line 49 in the relevant file to the specific skill. Please have a look 
	into the provided examples. 
	
(3) In the "src" folder: Tensorflow includes the code for executing local function approximators and controlling the Epsilon Greedy strategy
(4) In the "src" folder: Transfer_replay_memory controls the size of the replay memory, which is a SQL-Database. 


############
For further questions:
############
A German Word-Documented is provided, in which on of our research assistant wrote a short explanation how the Plant Simulation Model works internally. 

Please don't hesitate to contact any of the authors in case further support is needed. 
As this is a very complex implementation, please understand that a broader introduction cannot be given at the moment. 


