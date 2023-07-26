import subprocess
import time
import pandas as pd
import matplotlib.pyplot as plt

# Hadoop command to run the job
# Adjust this to your own job and arguments
hadoop_command = "hadoop jar myJob.jar com.mydomain.myJob /inputDir /outputDir"

# Start the timer
start_time = time.time()

# Run the Hadoop job
subprocess.check_call(hadoop_command, shell=True)

# Calculate the time taken
execution_time = time.time() - start_time
print(f"Execution time: {execution_time} seconds")

# Analyze the output
# Replace '/outputDir/part-r-00000' with your Hadoop job's output file
with open('/outputDir/part-r-00000', 'r') as f:
    data = f.read().split('\n')

# Assume the data is two columns of key-value pairs separated by a tab
# Adjust this according to your actual data format
data = [line.split('\t') for line in data if line]
df = pd.DataFrame(data, columns=['Key', 'Value'])

# Convert values to numeric
df['Value'] = pd.to_numeric(df['Value'])

# Plotting
# Adjust this to plot the graph you want
df.hist(column='Value', bins=20)
plt.show()
