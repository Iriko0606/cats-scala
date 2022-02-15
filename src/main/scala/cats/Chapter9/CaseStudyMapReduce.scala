//package cats.Chapter9
//
//import cats.implicits.catsKernelStdGroupForInt
//
//import scala.concurrent.ExecutionContext.Implicits.global
//import scala.concurrent.duration.DurationInt
//import scala.concurrent.{Await, Future}
//
//object CaseStudyMapReduce extends App {
//
//  /**
//    * 9 Case Study: Map-Reduce
//    *
//    * 今回のケーススタディでは、MonoidsやFunctorなどを使って、シンプルで強力な並列処理フレームワークを実装してみましょう。
//    *
//    * Hadoopを使ったことがある人や、ビッグデータに関わる仕事をしている人は、MapReduceを聞いたことがあるでしょう。
//    * MapReduceは、マシン（別名「ノード」）のクラスターで並列データ処理を行うためのプログラミングモデルです。
//    * その名が示すように、このモデルは、ScalaやFunctor型クラスでおなじみのマップ関数であるマップフェーズと、Scalaでは通常fold13と呼んでいるリデュースフェーズで構成されています。
//    *
//    * 9.1 Parallelizing map and fold
//    *
//    * mapの一般的なシグネチャーは、関数A => BをF[A]に適用して、F[B]を返すことです。
//    *
//    * == 図12 ==
//    *
//    * mapは，シーケンスの各要素を独立して変換します．
//    * 異なる要素に適用される変換の間には依存関係がないので，map を簡単に並列化することができます（
//    * 型に反映されない副作用を使用しないことを前提に，関数 A => B の型シグネチャがこれを示しています）。
//    *
//    * フォールドについてはどうでしょうか？
//    * このステップは、Foldableのインスタンスを使って実装できます。
//    * すべてのファンクタが foldable のインスタンスを持っているわけではありませんが、これらの型クラスを両方持っているデータ型の上にマップレデュースシステムを実装することができます。
//    * 還元ステップは、分散マップの結果に対する foldLeft となります。
//    *
//    * == 図13 ==
//    *
//    * reduceステップを分散させることで、探索の順序を制御できなくなります。
//    * 全体的なリダクションが完全に左から右に行われるとは限りません．
//    * いくつかのサブシーケンスを左から右にリダクションし，その結果を組み合わせることもできます．
//    * 正しさを確保するためには，連想的なリダクション操作が必要です．
//    *
//    * reduce(a1, reduce(a2, a3)) == reduce(reduce(a1, a2), a3)
//    *
//    * 連想性があれば，各ノードの部分列が初期データセットと同じ順序であれば，ノード間で任意に作業を分散することができます．
//    *
//    * 折りたたみ操作では、B 型の要素を計算の種にする必要があります。
//    * 折りたたみは任意の数の並列ステップに分割できるため、種が計算結果に影響を与えてはいけません。
//    * このため、当然、種はアイデンティティ要素でなければなりません。
//    *
//    * reduce(seed, a1) == reduce(a1, seed) == a1
//    *
//    * 要約すると、私たちの並列フォールドは、以下の場合に正しい結果をもたらします。
//    *
//    * - 還元関数が連想的であることを要求します。
//    * - この関数のIDを計算の種とします．
//    *
//    * このパターンはどのようなものでしょうか？
//    * そう、この本で最初に取り上げた型クラス「モノイド」に一周回って戻ってきたのです。
//    * モノイドの重要性を認識したのは私たちだけではありません。
//    * map-reduceジョブのmonoidデザインパターンは、TwitterのSummingbirdのような最近のビッグデータシステムの中核をなしています。
//    *
//    * このプロジェクトでは、非常にシンプルなシングルマシンのmap-reduceを実装していきます。
//    * まず、必要なデータフローをモデル化するために、foldMapというメソッドを実装します。
//    *
//    * 9.2 Implementing foldMap
//    *
//    * foldMapについては、Foldableを取り上げた際に簡単に説明しました。
//    * これは、foldLeft と foldRight の上に位置する派生操作の 1 つです。
//    * しかし、Foldable を使用するのではなく、map-reduce の構造を理解するのに役立つ foldMap をここで再実装します。
//    *
//    * まずは foldMap のシグネチャを書き出してみましょう。
//    * 以下のようなパラメータを受け付ける必要があります。
//    *
//    * - Vector[A]型のシーケンス。
//    * - A => B 型の関数（B に対応する Monoid が存在する）。
//    * 型のシグネチャを完成させるには、暗黙のパラメータやコンテキスト・バウンドを追加する必要があります。
//    *
//    * Exercise1
//    */
////    import cats.Monoid
////    def foldMap[A,B: Monoid](v: Vector[A])(f: A =>B): B = ???
//
//  /** 次に、foldMap の本体を実装します。
//    * 図14のフローチャートを参考に、必要な手順を考えてみましょう。
//    *
//    * 1.タイプAのアイテムのシーケンスから始める。
//    * 2.リストをマッピングして、B型のアイテムのシーケンスを生成します。
//    * 3.モノイドを使用して、アイテムを単一の B に減らす。
//    *
//    * == 図14 ==
//    *
//    * 参考までに出力例をご紹介します。
//    *
//    import cats.instances.int._ // for Monoid
//
//    foldMap(Vector(1, 2, 3))(identity)
//    // res1: Int = 6
//
//    import cats.instances.string._ // for Monoid
//
//    // Mapping to a String uses the concatenation monoid:
//    foldMap(Vector(1, 2, 3))(_.toString + "! ")
//    // res2: String = "1! 2! 3! "
//
//    // Mapping over a String to produce a String:
//    foldMap("Hello world!".toVector)(_.toString.toUpperCase)
//    // res3: String = "HELLO WORLD!"
//
//    * Exercise2
//    */
//  import cats.Monoid
//  import cats.syntax.semigroup._
//  def foldMap[A, B: Monoid](value: Vector[A])(f: A => B): B =
//    value.map(v => f(v)).foldLeft(Monoid[B].empty)(_ |+| _)
////  def foldMap[A, B: Monoid](as: Vector[A])(func: A => B): B =
////    as.foldLeft(Monoid[B].empty)(_ |+| func(_))
//  /** 9.3 Parallelising foldMap
//    *
//    * これで foldMap のシングルスレッド実装ができましたので、作業を分散して並列実行することを考えてみましょう。
//    * ここでは、シングルスレッド版の foldMap を構成要素として使用します。
//    *
//    * 図 15 に示すように、Map-Reduce クラスタでの作業の分配方法をシミュレートするマルチ CPU の実装を記述します。
//    *
//    * 1. 処理すべきすべてのデータの初期リストからスタートします。
//    * 2. データをバッチに分割し，各CPUに1バッチずつ送信する．
//    * 3. 各CPUは、バッチレベルのマップフェーズを並列に実行します。
//    * 4. CPUはバッチレベルのreduceフェーズを並列に実行し，各バッチのローカルな結果を生成します．
//    * 5. 各バッチの結果を1つの最終結果に還元する。
//    *
//    * == 図 15 ==
//    *
//    * Scalaには、作業をスレッド間で分散させるための簡単なツールがいくつか用意されています。
//    * 並列コレクションライブラリを使ってソリューションを実装することもできますが、もう少し深く掘り下げて、Futuresを使って自分でアルゴリズムを実装することにチャレンジしてみましょう。
//    *
//    * 9.3.1 Futures, Thread Pools, and ExecutionContexts
//    *
//    * Futuresのモナディックな性質については、すでにかなりのことがわかっています。
//    * ここでは、ScalaのFuturesが舞台裏でどのようにスケジューリングされているかを簡単におさらいしてみましょう。
//    *
//    * Futures は、暗黙の実行コンテキスト (ExecutionContext) パラメータによって決定されるスレッドプール上で動作します。
//    * Future.applyやその他のコンビネーターを使ってFutureを作成する際には、必ず暗黙のExecutionContextをスコープに入れておかなければなりません。
//    *
//    import scala.concurrent.Future
//    import scala.concurrent.ExecutionContext.Implicits.global
//
//    val future1 = Future {
//      (1 to 100).toList.foldLeft(0)(_ + _)
//    }
//    // future1: Future[Int] = Future(Success(5050))
//
//    val future2 = Future {
//      (100 to 200).toList.foldLeft(0)(_ + _)
//    }
//    // future2: Future[Int] = Future(Success(15150))
//    * この例では、ExecutionContext.Implicits.global をインポートしています。
//    * このデフォルトのコンテキストは、マシンの CPU ごとに 1 つのスレッドを持つスレッドプールを割り当てます。
//    * Future を作成すると、ExecutionContext はそれを実行するようにスケジュールします。
//    * スレッドプールに空きスレッドがあれば、Future は直ちに実行を開始します。
//    * 最近のマシンには少なくとも2つのCPUが搭載されているので、この例ではfuture1とfuture2が並行して実行されることになります。
//    *
//    * コンビネータの中には、他のFutureの結果に基づいて作業をスケジューリングする新しいFutureを作成するものがあります。
//    * たとえば、mapおよびflatMapメソッドは、入力値が計算され、CPUが利用可能になった時点で実行される計算をスケジューリングします。
//    *
//    val future3 = future1.map(_.toString)
//    // future3: Future[String] = Future(Success(5050))
//
//    val future4 = for {
//      a <- future1
//      b <- future2
//    } yield a + b
//    // future4: Future[Int] = Future(Success(20200))
//    *
//    * 7.2節で見たように、Future.sequenceを使ってList[Future[A]]をFuture[List[A]]に変換することができます。
//    * ↓はFutureのTraverseを使用している
//    Future.sequence(List(Future(1), Future(2), Future(3)))
//    // res6: Future[List[Int]] = Future(Success(List(1, 2, 3)))
//
//    * またはTraverseのインスタンスを使用します。
//
//    import cats.instances.future._ // for Applicative
//    import cats.instances.list._   // for Traverse
//    import cats.syntax.traverse._  // for sequence　↓はcats.syntax.traverse._のTraverseを使ってる
//
//    List(Future(1), Future(2), Future(3)).sequence
//    // res7: Future[List[Int]] = Future(Success(List(1, 2, 3)))
//
//    * いずれの場合も ExecutionContext が必要です。
//    * 最後に、Await.resultを使って、結果が出るまでFutureをブロックすることができます。
//  import scala.concurrent._
//  import scala.concurrent.duration._
//
//  Await.result(Future(1), 1.second) // wait for the result
//  // res8: Int = 1
//
//    * また、Cats.instances.futureからは、FutureのMonadとMonoidの実装が利用できます。
//    import cats.{Monad, Monoid}
//    import cats.instances.int._    // for Monoid
//    import cats.instances.future._ // for Monad and Monoid
//
//    Monad[Future].pure(42)
//
//    Monoid[Future[Int]].combine(Future(1), Future(2))
//
//    * 9.3.2 Dividing Work
//    * さて、Futuresについての記憶が蘇ったところで、作業をバッチに分割する方法を見てみましょう。
//    * マシン上で利用可能なCPUの数を調べるには、Java標準ライブラリのAPIコールを使用します。
//    *
//  Runtime.getRuntime.availableProcessors
//  // res11: Int = 2
//    * groupedメソッドを使って、シーケンス（Vectorを実装しているものなら何でも）を分割することができます。
//    * これを使って、作業のバッチを各CPUに分割することにします。
//    *
//  (1 to 10).toList.grouped(3).toList
//  // res12: List[List[Int]] = List(
//  //   List(1, 2, 3),
//  //   List(4, 5, 6),
//  //   List(7, 8, 9),
//  //   List(10)
//  // )
//    * 9.3.3 Implementing parallelFoldMap
//    *
//    * parallelFoldMapと呼ばれるfoldMapの並列バージョンを実装します。
//    * 型のシグネチャは以下の通りです。
//    def parallelFoldMap[A, B : Monoid]
//      (values: Vector[A])
//      (func: A => B): Future[B] = ???
//
//    * 上記の方法で作業をバッチに分割し、1つのCPUにつき1バッチとします。
//    * 各バッチを並列スレッドで処理します。
//    * アルゴリズムの全体像を確認する必要がある場合は、図 15 を参照してください。
//    * ボーナスポイントとして、上で説明した foldMap の実装を用いて各 CPU のバッチを処理してください。
//    *
//    * Exercise
//  **/
//  def parallelFoldMap[A, B: Monoid](values: Vector[A])(
//      func: A => B): Future[B] = {
//    // Calculate the number of items to pass to each CPU:
//    val numCores = Runtime.getRuntime.availableProcessors
//    val groupSize = (1.0 * values.size / numCores).ceil.toInt
//
//    // Create one group for each CPU:
//    val groups: Iterator[Vector[A]] =
//      values.grouped(groupSize)
//
//    // Create a future to foldMap each group:
//    val futures: Iterator[Future[B]] =
//      groups map { group =>
//        Future {
//        // ↑ 並列処理させたいからFuture
//          group.foldLeft(Monoid[B].empty)(_ |+| func(_))
//        }
//      }
//
//    // foldMap over the groups to calculate a final result:
//    Future.sequence(futures) map { iterable =>
//      iterable.foldLeft(Monoid[B].empty)(_ |+| _)
//    }
//  }
//
//  val result: Future[Int] =
//    parallelFoldMap((1 to 1000000).toVector)(identity)
//
//  Await.result(result, 1.second)
//  // res14: Int = 1784293664
//
//  /**
//    * 9.3.4 parallelFoldMap with more Cats
//    *
//    * 上では foldMap を独自に実装しましたが、このメソッドは項7.1で説明した Foldable 型クラスの一部としても利用可能です。
//    * Cats の Foldable および Traverseable 型クラスを使用して parallelFoldMap を再実装します。
//    * Exercise
//    */
//  import cats.Monoid
//
//  import cats.instances.int._ // for Monoid
//  import cats.instances.future._ // for Applicative and Monad
//  import cats.instances.vector._ // for Foldable and Traverse
//
//  import cats.syntax.foldable._ // for combineAll and foldMap
//  import cats.syntax.traverse._ // for traverse
//
//  import scala.concurrent._
//  import scala.concurrent.duration._
//  import scala.concurrent.ExecutionContext.Implicits.global
//  def parallelFoldMap2[A, B: Monoid](values: Vector[A])(
//      func: A => B): Future[B] = {
//    // Calculate the number of items to pass to each CPU:
//    val numCores = Runtime.getRuntime.availableProcessors
//    val groupSize = (1.0 * values.size / numCores).ceil.toInt
//
//    values
//      .grouped(groupSize)
//      .toVector
//      .traverse(group => Future(group.toVector.foldMap(func)))
//      .map(_.combineAll)
//  }
//
//  val result2: Future[Int] =
//    parallelFoldMap((1 to 1000000).toVector)(identity)
//
//  Await.result(result2, 1.second)
//
//  /**
//  * vector.grouped の呼び出しは Iterable[Iterator[Int]] を返します。コード中にtoVectorの呼び出しを散りばめて、Catsが理解できる形にデータを変換しています。
//  * traverseの呼び出しは、バッチごとに1つのIntを含むFuture[Vector[Int]]を作成します。
//  * map の呼び出しでは、Foldable の combineAll メソッドを使用して一致するデータを結合します。
//  *
//  * 9.4 Summary
//  *
//  * 今回の事例では、クラスター上で実行されるmap-reduceを模倣したシステムを実装しました。
//  * アルゴリズムは3つのステップで構成されています。
//  * 1. データをバッチ処理し、各 "ノード "に1つのバッチを送信する。
//  * 2. 各バッチに対してローカルなMap-Reduceを実行する。
//  * 3. 結果をモノイド加算で結合する。
//  *
//  * このトイシステムは、Hadoopのような実世界のMap-Reduceシステムのバッチ処理をエミュレートしています。
//  * しかし、実際には、ノード間の通信がごくわずかな1台のマシンですべての作業を実行しています。
//  * リストを効率的に並列処理するために、実際にはデータをバッチ処理する必要はありません。
//  * s単純に，Functorを使ってマッピングし，Monoidを使ってリダクションすればよいのです．
//  *
//  * バッチ処理の方法に関わらず、モノイドを使ったマッピングとリダクションは強力で一般的なフレームワークであり、加算や文字列連結などの単純なタスクに限定されません。
//  * データサイエンティストが日々の分析で行っているタスクのほとんどは、モノイドとしてキャストすることができます。以下のすべてのモノイドがあります。
//  *
//  * - ブルームフィルタのような近似集合
//  * - HyperLogLogアルゴリズムなどの集合カーディナリティ推定器
//  * - ベクトル，および確率的勾配降下法などのベクトル演算
//  * - t-digestのような分位推定法
//  * などの分位値推定などがあります。
//  */
//
//}
