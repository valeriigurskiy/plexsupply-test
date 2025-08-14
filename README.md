# A console-based, multi-threaded program to calculate the factorial for each number from an input file

The main idea behind this project is to read data from an input file (data/input.txt) and calculate the factorial for each number.

The output should be generated in the same order as the numbers appear in the input file.

I decided to work with ```Semaphore```, ```ReentrantLock```, ```Condition``` and ```BlockingQueue``` to manage multithreading aspects

* ```Semaphore``` is used for controlling access to shared resources (writing in ```results``` variable) by limiting operation up to ```MAX_OPERATIONS_PER_SECOND/sec```. In our case, ```MAX_OPERATIONS_PER_SECOND``` = ```100```

* ```ReentrantLock``` ensures that only one thread can operate with ```results``` at any time

* ```Condition``` used for providing communication between threads (specifically between ```writerTask``` and ```calculateFactorial``` methods)

* ```LinkedBlockingQueue``` stores tasks for factorial calculation.
