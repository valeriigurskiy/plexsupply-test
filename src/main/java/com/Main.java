package com;

import java.util.Scanner;

public class Main {

    private static final String INPUT_FILE_PATH = "data/input.txt";
    private static final String OUTPUT_FILE_PATH = "data/output.txt";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter pool size: ");
        String inputLine = scanner.nextLine();

        int poolSize = parsePoolSizeParam(inputLine);

        scanner.close();

        FactorialCalculator calculator = new FactorialCalculator(poolSize, INPUT_FILE_PATH, OUTPUT_FILE_PATH);
        calculator.start();
    }

    private static int parsePoolSizeParam(String inputLine) {
        try {
            return Integer.parseInt(inputLine.trim());
        } catch (NumberFormatException e) {
            throw new UnsupportedOperationException("Pool size must be positive integer.");
        }
    }

}
