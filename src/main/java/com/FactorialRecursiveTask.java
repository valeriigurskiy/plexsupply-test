package com;

import java.math.BigInteger;
import java.util.concurrent.RecursiveTask;

public class FactorialRecursiveTask extends RecursiveTask<BigInteger> {

    private final int start;
    private final int end;

    public FactorialRecursiveTask(int start, int end) {
        this.start = start;
        this.end = end;
    }

    @Override
    protected BigInteger compute() {
        if (start == end) {
            return BigInteger.valueOf(start);
        } else {
            int mid = (start + end) / 2;
            FactorialRecursiveTask leftTask = new FactorialRecursiveTask(start, mid);
            FactorialRecursiveTask rightTask = new FactorialRecursiveTask(mid + 1, end);

            leftTask.fork();
            BigInteger rightResult = rightTask.compute();
            BigInteger leftResult = leftTask.join();

            return leftResult.multiply(rightResult);
        }
    }

}
