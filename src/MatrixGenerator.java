import org.json.JSONObject;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Random;

class MatrixGenerator {
  static int VALUES_BOUND = 1000000;

  static class MatrixBuilder implements Runnable {
    int _rows;
    int _columns;
    long _seed;
    boolean _isSimple;
    MatrixOperation _resultMatrix;

    MatrixBuilder(int rows, int columns, long seed, boolean isSimple) {
      _rows = rows;
      _columns = columns;
      _seed = seed;
      _isSimple = isSimple;
    }

    MatrixOperation getResultMatrix() {
      return _resultMatrix;
    }

    public void run () {
      int size = _rows * _columns;
      long[] array = new long[size];
      Random random = new Random(_seed);

      for (int i = 0; i < size; ++i) {
        array[i] = random.nextInt(VALUES_BOUND * 2) - VALUES_BOUND;
      }

      if (_isSimple) {
        //_resultMatrix = new SimpleMatrix(_rows, _columns, array);
      } else {
        _resultMatrix = new CustomMatrix(_rows, _columns, array);
      }
    }
  }

  static MatrixOperation[] createMatrices(String fromFileName, boolean isSimple) {
    String jsonString = "";
    MatrixOperation[] matrices = new MatrixOperation[2];

    try (BufferedReader br = new BufferedReader(new FileReader(fromFileName))) {
      StringBuilder sb = new StringBuilder();
      String line = br.readLine();

      while (line != null) {
        sb.append(line);
        sb.append(System.lineSeparator());
        line = br.readLine();
      }

      jsonString = sb.toString();
    } catch (Exception e) {
      System.err.println("The 2 matrices should be stored in a JSON file, which path should be passed as an argument");
      System.exit(1);
    }

    try {
      JSONObject jsonObj = new JSONObject(jsonString);
      MatrixBuilder[] matrixBuilders = new MatrixBuilder[2];
      Thread[] threads = new Thread[2];

      int rows1 = jsonObj.getInt("rows1");
      int columns1 = jsonObj.getInt("columns1");
      int rows2 = jsonObj.getInt("rows2");
      int columns2 = jsonObj.getInt("columns2");
      long seed = jsonObj.getInt("seed");

      if (rows1 < 1 || columns1 < 1 || rows2 < 1 || columns2 < 1) {
        System.err.print("The \"rows\" and \"columns\" properties of the JSON files must have a value of at least 1");
        System.exit(1);
      }
      if (columns1 != rows2 && columns2 != rows1) {
        System.err.print("One of the two matrices' \"columns\" must match the other matrix's \"rows\""
          + "for the multiplication to be doable");
        System.exit(1);
      }

      try {
        matrixBuilders[0] = new MatrixBuilder(rows1, columns1, seed, isSimple);
        matrixBuilders[1] = new MatrixBuilder(rows2, columns2, seed, isSimple);
        threads[0] = new Thread(matrixBuilders[0]);
        threads[1] = new Thread(matrixBuilders[1]);
        threads[0].start();
        threads[1].start();
        threads[0].join();
        threads[1].join();
      } catch (InterruptedException e) {
        System.err.println("Thread supposed to parse matrix has been unexpectedly interrupted");
      }

      matrices[0] = matrixBuilders[0].getResultMatrix();
      matrices[1] = matrixBuilders[1].getResultMatrix();
    } catch (JSONException e) {
      System.err.println("The JSON file should contain 3 properties: \"rows\", \"columns\" and \"seed\""
        + " (the latter being the seed from which the random numbers are generated to fill the matrices)");
      System.exit(1);
    }

    return matrices;
  }
}
