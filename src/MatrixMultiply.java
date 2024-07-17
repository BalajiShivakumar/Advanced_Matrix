class MatrixProcessor {
  static void calculate(String[] arguments, boolean useSimpleMatrix) {
    if (arguments.length < 1 || (arguments.length == 1 && !arguments[0].contains("json"))) {
      System.err.println("Usage: java -cp \"out;libs/*\" MatrixProcessor <path-to.json> [-v]");
      System.exit(1);
    }

    boolean verbose = false;
    String filePath = arguments[0];

    if (arguments.length > 1 && (arguments[0].equals("-v") || arguments[0].equals("--verbose")
      || arguments[1].equals("-v") || arguments[1].equals("--verbose"))) {
      verbose = true;
    }
    if (arguments.length > 1 && (arguments[0].equals("-v") || arguments[0].equals("--verbose"))) {
      filePath = arguments[1];
    }

    Timer timer1 = new Timer();
    timer1.start();
    MatrixOperation[] matrices = MatrixGenerator.createMatrices(filePath, useSimpleMatrix);
    timer1.end();
    System.out.println("Time spent generating matrices: " + timer1.getEllapsedTime() + "milliseconds");
    if (verbose) {
      matrices[0].printForParsing();
      matrices[1].printForParsing();
    }

    if (matrices[0].getColumns() != matrices[1].getRows()) {
      System.out.println("omitting matrix1 * matrix2 is not impossible");
    } else {
      Timer timer2 = new Timer();
      System.out.println("calculating matrix1 * matrix2");
      timer2.start();
      MatrixOperation result1 = matrices[0].multiply(matrices[1]);
      timer2.end();
      System.out.println("Time spent computing m1 * m2: " + timer2.getElapsedTime() + "millisecond");
      if (verbose) {
        result1.printMatrix();
      }
    }

    if (matrices[1].getColumns() != matrices[0].getRows()) {
      System.out.println("omitting matrix2 * matrix1 is not possible");
    } else if (matrices[0].getColumns() == matrices[1].getRows() && matrices[0].getColumns() == matrices[1].getColumns()) {
      System.out.println("omitting matrix2 * matrix1 is equal matrix1 * matrix2");
    } else {
      Timer timer3 = new Timer();
      System.out.println("calculating matrix2 * matrix1");
      timer3.start();
      MatrixOperation result2 = matrices[1].multiply(matrices[0]);
      timer3.end();
      System.out.println("Time spent calcilating matrix2 * matrix1: " + timer3.getEllapsedTime() + "milliseconds");
      if (verbose) {
        result2.printMatrix();
      }
    }
  }
}
