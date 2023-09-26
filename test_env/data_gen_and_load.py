import os
import socket
import subprocess
import threading
import time

hostname = socket.gethostname()
bootstrap_port = os.environ['BOOTSTRAP_PORT']


def start_producing():
    # now we spin up process calling producer script and pushing messages

    with subprocess.Popen(
            f'/usr/bin/kafka-console-producer --bootstrap-server localhost:{bootstrap_port} --topic {hostname}.test.data',
            shell=True, stdin=subprocess.PIPE) as producer_process:
        with open('test_text.txt') as msg_file:
            for line in msg_file:
                #print(line)
                producer_process.stdin.write(bytes(line, 'utf-8'))
                producer_process.stdin.flush()
                print(f'Producer sent message:{line}')
                time.sleep(15)  #sleep a minute before next read


if __name__ == "__main__":
    os.system(f'kafka-topics --bootstrap-server localhost:{bootstrap_port} --create --topic {hostname}.test.data')

    producer_thread = threading.Thread(target=start_producing)
    #the below is blocking I think std out io, maybe we try separate process
    #c1_thread = threading.Thread(target=start_consuming(f'{hostname}_consumer1'))
    #c2_thread = threading.Thread(target=start_consuming(f'{hostname}_consumer2'))
    producer_thread.start()
    #maybe try os.system, we can't identify each consumer but who cares, let it output to stdout and we will do 2
    #consumers and should work for load system
    os.system(f'/usr/bin/kafka-console-consumer --bootstrap-server localhost:{bootstrap_port} --topic {hostname}.test.data --group testGroup1')
    # 2 at a time doesn't really work either we will go with 1
    #os.system(f'/usr/bin/kafka-console-consumer --bootstrap-server localhost:{bootstrap_port} --topic {hostname}.test.data')



