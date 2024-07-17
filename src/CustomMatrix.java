import java.util.Arrays;

class CustomMatrix extends MatrixOperation {
  private int SPLIT_SIZE = 4;
  private int LEAF_SIZE = 256;

  private int _resultHeight;
  private int _resultWidth;
  private int _smallSize;

  private long[][][] _splitArrays;
  private boolean[][] _areSplitArraysEmpty;
  private MatrixOperation[][] _AB;

  private MatrixOperation[] _M;
  private MatrixOperation[] _C;

  private CustomMatrix() {
    super();
  }

  CustomMatrix(int height, int width, long[] array) {
    super(height, width, array);
  }

  private CustomMatrix(int sideSize, long[] array) {
    super(sideSize, sideSize, array);

    _resultHeight = sideSize;
    _resultWidth = sideSize;
    _smallSize = sideSize / 2;
  }

  class MatrixSplitter implements Runnable {
    int _start;
    int _end;
    int _matrixId;
    MatrixOperation _m;

    MatrixSplitter(int start, int end, int matrixId, MatrixOperation m) {
      _start = start;
      _end = end;
      _matrixId = matrixId;
      _m = m;
    }

    public void run() {
      for (int i = _start; i < _end; ++i) {
        for (int j = 0; j < _smallSize; ++j) {
          int recipientIdx = i * _smallSize + j;

          for (int k = 0; k < SPLIT_SIZE; ++k) {
            int newI = i + (k / 2 * _smallSize);
            int newJ = k != 0 && k != 2 ? j + _smallSize : j;

            if (newI < _m.getRows() && newJ < _m.getColumns()) {
              long value = _m.getData()[newI * _m.getColumns() + newJ];

              _splitArrays[_matrixId][k][recipientIdx] = value;

              if (value != 0 && _areSplitArraysEmpty[_matrixId][k]) {
                _areSplitArraysEmpty[_matrixId][k] = false;
              }
            } else {
              _splitArrays[_matrixId][k][recipientIdx] = 0;
            }
          }
        }
      }
    }
  }

  class SubMatrixCalculator implements Runnable {
    int _start;
    int _end;

    SubMatrixCalculator(int start, int end) {
      _start = start;
      _end = end;
    }

    private void compute(int index) {
      switch (index) {
        case 0:
          _M[0] = (_AB[0][0].add(_AB[0][3])).multiply(_AB[1][0].add(_AB[1][3]), false);
          break;
        case 1:
          _M[1] = (_AB[0][2].add(_AB[0][3])).multiply(_AB[1][0], false);
          break;
        case 2:
          _M[2] = _AB[0][0].multiply(_AB[1][1].subtract(_AB[1][3]), false);
          break;
        case 3:
          _M[3] = _AB[0][3].multiply(_AB[1][2].subtract(_AB[1][0]), false);
          break;
        case 4:
          _M[4] = (_AB[0][0].add(_AB[0][1])).multiply(_AB[1][3], false);
          break;
        case 5:
          _M[5] = (_AB[0][2].subtract(_AB[0][0])).multiply(_AB[1][0].add(_AB[1][1]), false);
          break;
        case 6:
          _M[6] = (_AB[0][1].subtract(_AB[0][3])).multiply(_AB[1][2].add(_AB[1][3]), false);
          break;
        default:
      }
    }

    public void run() {
      for(int i = _start; i < _end; ++i) {
        compute(i);
      }
    }
  }

  class FinalSubMatrixCalculator implements Runnable {
    int _start;
    int _end;

    FinalSubMatrixCalculator(int start, int end) {
      _start = start;
      _end = end;
    }

    private void compute(int index) {
      switch (index) {
        case 0:
          _C[0] = _M[0].add(_M[3]).subtract(_M[4]).add(_M[6]);
          break;
        case 1:
          _C[1] = _M[2].add(_M[4]);
          break;
        case 2:
          _C[2] = _M[1].add(_M[3]);
          break;
        case 3:
          _C[3] = _M[0].subtract(_M[1]).add(_M[2]).add(_M[5]);
          break;
        default:
      }
    }

    public void run() {
      for(int i = _start; i < _end; ++i) {
        compute(i);
      }
    }
  }

  MatrixOperation add(MatrixOperation matrix2) {
    if (this.isEmpty()) {
      return matrix2;
    }
    if (matrix2.isEmpty()) {
      return this;
    }

    int sideSize = this.getRows();
    long[] resultData = new long[sideSize * sideSize];

    for (int i = 0; i < sideSize; ++i) {
      for (int j = 0; j < sideSize; ++j) {
        int index = i * sideSize + j;

        resultData[index] = this.getData()[index] + matrix2.getData()[index];
      }
    }

    return new CustomMatrix(sideSize, resultData);
  }

  MatrixOperation subtract(MatrixOperation matrix2) {
    if (matrix2.isEmpty()) {
      return this;
    }
    if (this.isEmpty()) {
      return ((CustomMatrix)(matrix2)).multiplyByInt(-1);
    }

    int sideSize = this.getRows();
    long[] resultData = new long[sideSize * sideSize];

    for (int i = 0; i < sideSize; ++i) {
      for (int j = 0; j < sideSize; ++j) {
        int index = i * sideSize + j;

        resultData[index] = this.getData()[index] - matrix2.getData()[index];
      }
    }

    return new CustomMatrix(sideSize, resultData);
  }

  private static int getNextPowerOfTwo(int num) {
    int power = 1;

    while (power < num) {
      power *= 2;
    }

    return power;
  }

  private int getsmallSize(int height1, int width1, int height2, int width2) {
    int m1GreaterSide = height1 > width1 ? height1 : width1;
    int m2GreaterSide = height2 > width2 ? height2 : width2;
    int greaterSide = m1GreaterSide > m2GreaterSide ? m1GreaterSide : m2GreaterSide;

    return getNextPowerOfTwo(greaterSide) / (SPLIT_SIZE / 2);
  }

  private void runParallelSplit(int matrixId, MatrixOperation m, int numThreads, int nbWorkToDo) {
    Thread[] threads = new Thread[numThreads];
    int sectionSize = (nbWorkToDo / numThreads) + (nbWorkToDo % numThreads != 0 ? 1 : 0);

    for (int i = 0; i < numThreads; ++i) {
      int start = i * sectionSize;
      int end = (i == numThreads - 1 ? nbWorkToDo : (i + 1) * sectionSize);

      threads[i] = new Thread(new MatrixSplitter(start, end, matrixId, m));
      threads[i].start();
    }

    try {
      for (int i = 0; i < numThreads; ++i) {
        threads[i].join();
      }
    } catch (InterruptedException e) {
      System.err.println("Thread supposed to compute line has been unexpectedly interrupted");
    }
  }

  private void runSequentialSplit(int matrixId, MatrixOperation m, int nbWorkToDo) {
    new MatrixSplitter(0, nbWorkToDo, matrixId, m).run();
  }

  private void createBlockMatricesFromSplitArrays() {
    for (int i = 0; i < 2; ++i) {
      for (int j = 0; j < SPLIT_SIZE; ++j) {
        _AB[i][j] = !_areSplitArraysEmpty[i][j] ?
          new CustomMatrix(_smallSize, _splitArrays[i][j]) :
          new CustomMatrix();
      }
    }
  }

  // No check on empty matrices because already checked in multiply()
  private void splitMatrices(MatrixOperation matrix1, MatrixOperation matrix2) {
    if (_smallSize * 2 <= LEAF_SIZE) {
      _AB = null;
      return;
    }

    _splitArrays = new long[2][SPLIT_SIZE][_smallSize * _smallSize];
    _areSplitArraysEmpty = new boolean[2][SPLIT_SIZE];
    Arrays.fill(_areSplitArraysEmpty[0], true);
    Arrays.fill(_areSplitArraysEmpty[1], true);

    int nbWorkToDo = _smallSize;
    int numThreads = nbWorkToDo * 2 > AVAILABLE_THREADS ? AVAILABLE_THREADS : nbWorkToDo * 2;

    if (numThreads > 1) {
      runParallelSplit(0, matrix1, numThreads, nbWorkToDo);
    } else {
      runSequentialSplit(0, matrix1, nbWorkToDo);
    }
    if (numThreads > 1) {
      runParallelSplit(1, matrix2, numThreads, nbWorkToDo);
    } else {
      runSequentialSplit(1, matrix2, nbWorkToDo);
    }

    _AB = new MatrixOperation[2][SPLIT_SIZE];
    createBlockMatricesFromSplitArrays();
  }

  private void runParallelComputeM(int numThreads, int nbWorkToDo) {
    Thread[] threads = new Thread[numThreads];
    int sectionSize = (nbWorkToDo / numThreads) + (nbWorkToDo % numThreads != 0 ? 1 : 0);

    for (int i = 0; i < numThreads; ++i) {
      int start = i * sectionSize;
      int end = (i == numThreads - 1 ? nbWorkToDo : (i + 1) * sectionSize);

      threads[i] = new Thread(new SubMatrixCalculator(start, end));
      threads[i].start();
    }

    try {
      for (int i = 0; i < numThreads; ++i) {
        threads[i].join();
      }
    } catch (InterruptedException e) {
      System.err.println("Thread supposed to compute line has been unexpectedly interrupted");
    }
  }

  private void runSequentialComputeM(int nbWorkToDo) {
    new SubMatrixCalculator(0, nbWorkToDo).run();
  }

  private void calculate() {
    int nbWorkToDo = 7;
    int numThreads = nbWorkToDo > AVAILABLE_THREADS ? AVAILABLE_THREADS : nbWorkToDo;
    _M = new MatrixOperation[nbWorkToDo];

    if (numThreads > 1) {
      runParallelComputeM(numThreads, nbWorkToDo);
    } else {
      runSequentialComputeM(nbWorkToDo);
    }
  }

  private void runParallelComputeC(int numThreads, int nbWorkToDo) {
    Thread[] threads = new Thread[numThreads];
    int sectionSize = (nbWorkToDo / numThreads) + (nbWorkToDo % numThreads != 0 ? 1 : 0);

    for (int i = 0; i < numThreads; ++i) {
      int start = i * sectionSize;
      int end = (i == numThreads - 1 ? nbWorkToDo : (i + 1) * sectionSize);

      threads[i] = new Thread(new FinalSubMatrixCalculator(start, end));
      threads[i].start();
    }

    try {
      for (int i = 0; i < numThreads; ++i) {
        threads[i].join();
      }
    } catch (InterruptedException e) {
      System.err.println("Thread supposed to compute line has been unexpectedly interrupted");
    }
  }

  private void runSequentialComputeC(int nbWorkToDo) {
    new FinalSubMatrixCalculator(0, nbWorkToDo).run();
  }

  private void calculate_C() {
    int nbWorkToDo = 4;
    int numThreads = nbWorkToDo > AVAILABLE_THREADS ? AVAILABLE_THREADS : nbWorkToDo;
    _C = new MatrixOperation[nbWorkToDo];

    if (numThreads > 1) {
      runParallelComputeC(numThreads, nbWorkToDo);
    } else {
      runSequentialComputeC(nbWorkToDo);
    }
  }

  // No check on empty matrices because empty matrices won't be split - and therefore won't have to be merged
  private MatrixOperation mergeMatricesBlocks() {
    long[] resultData = new long[_resultHeight * _resultWidth];

    for (int i = 0; i < _resultHeight; ++i) {
      for (int j = 0; j < _resultWidth; ++j) {
        int sendingMatrixId = (j >= _smallSize ? 1 : 0) + (i >= _smallSize ? 2 : 0);
        int sendingMatrixIndex =
          (i - (sendingMatrixId >= 2 ? _smallSize : 0)) * _smallSize
            + j - (sendingMatrixId == 1 || sendingMatrixId == 3 ? _smallSize : 0);

        resultData[i * _resultWidth + j] = !_C[sendingMatrixId].isEmpty() ?
          _C[sendingMatrixId].getData()[sendingMatrixIndex] :
          0;
      }
    }

    return new CustomMatrix(_resultHeight, _resultWidth, resultData);
  }

  // No check on empty matrices because already checked in subtract()
  private MatrixOperation multiplyByInt(int num) {
    int sideSize = this.getRows();
    long[] resultData = new long[sideSize * sideSize];

    for (int i = 0; i < sideSize; ++i) {
      for (int j = 0; j < sideSize; ++j) {
        int index = i * sideSize + j;

        resultData[index] = this.getData()[index] * num;
      }
    }

    return new CustomMatrix(sideSize, resultData);
  }

  private MatrixOperation simpleSquareMultiply(MatrixOperation matrix2) {
    int resultSideSize = this.getRows();
    long[] resultData = new long[resultSideSize * resultSideSize];

    for (int i = 0; i < resultSideSize; ++i) {
      for (int k = 0; k < resultSideSize; ++k) {
        for (int j = 0; j < resultSideSize; ++j) {
          resultData[i * resultSideSize + j] +=
            this.getData()[i * resultSideSize + k] * matrix2.getData()[k * resultSideSize + j];
        }
      }
    }

    return new CustomMatrix(resultSideSize, resultData);
  }

  private MatrixOperation simpleNotSquareMultiply(MatrixOperation matrix2) {
    int resultRow = this.getRows();
    int resultColumn = matrix2.getColumns();
    long[] resultData = new long[resultRow * resultColumn];

    for (int i = 0; i < resultRow; ++i) {
      for (int k = 0; k < this.getColumns(); ++k) {
        for (int j = 0; j < resultColumn; ++j) {
          resultData[i * resultColumn + j] +=
            this.getData()[i * this.getColumns() + k] * matrix2.getData()[k * resultColumn + j];
        }
      }
    }

    return new CustomMatrix(resultRow, resultColumn, resultData);
  }

  // No check on empty matrices because already checked in multiply()
  private MatrixOperation simpleMultiply(MatrixOperation matrix2) {
    if (this.getRows() == this.getColumns()) {
      return this.simpleSquareMultiply(matrix2);
    }

    return this.simpleNotSquareMultiply(matrix2);
  }

  MatrixOperation multiply(MatrixOperation matrix2) {
    if (this.isEmpty() || matrix2.isEmpty()) {
      return new CustomMatrix();
    }

    if (_smallSize == 0) {
      _resultHeight = this.getRows();
      _resultWidth = matrix2.getColumns();
      _smallSize = getsmallSize(this.getRows(), this.getColumns(), matrix2.getRows(), matrix2.getColumns());
    }

    Timer timer2 = new Timer();
    timer2.start();

    splitMatrices(this, matrix2);

    timer2.end();
    long time2 = timer2.getEllapsedTime();
    if (time2 != 0 && AVAILABLE_THREADS != 0) {
      //System.out.println("Time spent splitting matrices: " + time2 + "ms");
    }

    if (_AB == null) {
      return this.simpleMultiply(matrix2);
    }

    Timer timer3 = new Timer();
    timer3.start();

    calculate();

    timer3.end();
    long time3 = timer3.getEllapsedTime();
    if (time3 != 0) {
      //System.out.println("Time spent computing M: " + time3 + "ms");
    }

    calculate_C();

    Timer timer5 = new Timer();
    timer5.start();

    MatrixOperation result = mergeMatricesBlocks();

    timer5.end();
    long time5 = timer5.getEllapsedTime();
    if (time5 != 0) {
      //System.out.println("Time spent merging matrices: " + time5 + "ms");
    }

    return result;
  }
}
