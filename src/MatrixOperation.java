abstract class MatrixOperation {
  int AVAILABLE_THREADS = Runtime.getRuntime().availableProcessors();

  private int _rows;
  private int _columns;
  private long[] _data;

  MatrixOperation(int rows, int columns, long[] data) {
    _rows = rows;
    _columns = columns;
    _data = data;
  }

  MatrixOperation() {}

  int getRows() {
    return _rows;
  }

  int getColumns() {
    return _columns;
  }

  long[] getData() {
    return _data;
  }

  boolean isEmpty() {
    return _data == null;
  }

  void printMatrix() {
    for (int i = 0; i < _data.length; ++i) {
      if (i != 0) {
        if (i % _columns == 0) {
          System.out.println();
        } else {
          System.out.print(", ");
        }
      }

      long value = _data[i];
      int decimals = 0;

      while (value >= 10 || value <= -10) {
        decimals += 1;
        value /= 10;
      }

      String spaces = "";
      for (int j = decimals; j < 5; ++j) {
        spaces += " ";
      }
      if (value >= 0) {
        spaces += " ";
      }

      System.out.print(spaces);
      System.out.print(_data[i]);
    }

    System.out.println();
    System.out.println();
  }

  void printForParsing() {
    for (int i = 0; i < _data.length; ++i) {
      if (i != 0) {
        if (i % _columns == 0) {
          System.out.println();
        } else {
          System.out.print(", ");
        }
      }

      System.out.print(_data[i]);
    }

    System.out.println();
    System.out.println();
  }

  abstract MatrixOperation add(MatrixOperation m2);
  abstract MatrixOperation subtract(MatrixOperation m2);
  abstract MatrixOperation multiply(MatrixOperation m2);

  MatrixOperation multiply(MatrixOperation m2, boolean doMultiThread) {
    if (!doMultiThread) {
      AVAILABLE_THREADS = 0;
    }

    return this.multiply(m2);
  }
}
