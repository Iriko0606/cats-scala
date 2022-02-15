package cats.Chapter7

import cats.implicits._
import cats.Applicative
import cats.implicits.{catsStdInstancesForFuture, catsSyntaxApplicativeId}

object FoldableAndTraverse extends App {

  /**
    * 7 Foldable and Traverse
    *
    * この章では、コレクションの反復処理を実現する2つの型クラスを見てみましょう。
    *
    * - Foldable は、おなじみのfoldLeft と foldRight の操作を抽象化したものです。
    * - Traverse は、Applicatives を使用して折り畳みよりも少ない負担で反復処理を行う、より高いレベルの抽象化です。
    *
    * まず、Foldableについて見ていき、折り畳みが複雑になる場合やTraverseが便利になる場合を検討していきます。
    *
    * 7.1 Foldable
    *
    * Foldable タイプのクラスは、リスト、ベクター、ストリームなどのシーケンスでおなじみの foldLeft および foldRight メソッドをキャプチャします。
    * Foldable を使用すると、さまざまな種類のシーケンスで動作する汎用的な折り畳みを書くことができます。
    * また、新しいシーケンスを作成してコードに組み込むこともできます。
    * Foldableは、モノイドやEvalモナドの素晴らしい使用例を提供してくれます。
    *
    * 7.1.1 Folds and Folding
    *
    * まず、折りたたみの一般的な概念を簡単に説明します。
    * アキュムレータの値と，それをシーケンスの各項目と結合するためのバイナリ関数を与えます．
    *
    * def show[A](list: List[A]): String =
    *   list.foldLeft("nil")((accum, item) => s"$item then $accum")show(Nil)
    *   // res0: String = "nil"
    *   show(List(1, 2, 3))
    *   // res1: String = "3 then 2 then 1 then nil"
    *
    *   foldLeft メソッドは、シーケンスを再帰的に処理します。
    *   バイナリ関数が各項目に対して繰り返し呼び出され、各呼び出しの結果が次の項目のアキュムレータになります。
    *   シーケンスの最後に到達すると、最後のアキュムレータが最終的な結果になります。
    *
    *   行う作業によっては、折る順序が重要になることがあります。
    *   そのため、折り方には2つの標準的なバリエーションがあります。
    *
    *   - foldLeftは、"左 "から "右 "へ（開始から終了まで）トラバースします。
    *   - foldRightは、"右 "から "左 "へ（フィニッシュからスタートへ）トラバースします。
    *   図11はそれぞれの方向性を示したものです。
    *
    *   == 図11 ==
    *
    *   foldLeft と foldRight は、二項演算が連想的である場合は同等です。
    *   たとえば、0 をアキュムレータとして使用し、加算を演算として使用して、どちらかの方向に折りたたむことで List[Int] の合計を得ることができます。
    *
    *   List(1, 2, 3).foldLeft(0)(_ + _)
    *   // res2: Int = 6
    *   List(1, 2, 3).foldRight(0)(_ + _)
    *   // res3: Int = 6
    *
    *　非結合演算子を用意した場合、評価の順序に違いが生じます。
    * 例えば、減算を使って折り返すと、それぞれの方向で異なる結果が得られます。
    * この章では、コレクションの反復処理を実現する2つの型クラスを見てみましょう。
    * Foldable は、おなじみの foldLeft と foldRight の操作を抽象化したものです。
    * Traverse は、Applicatives を使用して折り畳みよりも少ない負担で反復処理を行う、より高いレベルの抽象化です。
    * まず、Foldableについて見ていき、折り畳みが複雑になる場合やTraverseが便利になる場合を検討していきます。
    *
    * List(1, 2, 3).foldLeft(0)(_ - _)
    * // res4: Int = -6
    * List(1, 2, 3).foldRight(0)(_ - _)
    * // res5: Int = 2
    *
    * 7.1.2 Exercise: Reflecting on Folds
    *
    * 空のリストをアキュムレータに、 :: を二項演算子にして、 foldLeft および foldRight を使ってみてください。
    * それぞれの場合、どのような結果が得られるでしょうか？
    */
  println(List(1, 2, 3).foldLeft(List.empty[Int])((a, i) => i :: a))
  // 321
  println(List(1, 2, 3).foldRight(List.empty[Int])((a, i) => a :: i))
  //123
  /**
    * 型エラーを避けるために，アキュムレータの型を慎重に指定しなければならないことに注意してください．
    * ここでは List.empty[Int] を使用して，アキュムレータの型が Nil.type や List[Nothing] と推測されないようにしています．
    *
    * List(1, 2, 3).foldRight(Nil)(_ :: _)
    * // error: type mismatch;
    * //  found   : List[Int]
    * //  required: scala.collection.immutable.Nil.type
    * // List(1, 2, 3).foldRight(Nil)(_ :: _)
    * //
    *
    * 7.1.3 Exercise: Scaf-fold-ing Other Methods
    *
    * foldLeft と foldRight は非常に一般的なメソッドです。
    * これらを使って、私たちが知っている他の高レベルなシーケンス操作の多くを実装することができます。
    * リストの map、flatMap、filter、sum メソッドの代わりに foldRight を使って実装することで、このことを実感してください。
    */
  def map[A, B](l: List[A])(f: A => B): List[B] = {
    l.foldRight(List.empty[B]) { (i, accum) =>
      f(i) :: accum
    }
  }

  def flatMap[A, B](l: List[A])(f: A => List[B]): List[B] = {
    l.foldRight(List.empty[B]) { (i, accum) =>
      f(i) ::: accum
    }
  }

  def filter[A](l: List[A])(f: A => Boolean): List[A] = {
    l.foldRight(List.empty[A]) { (i, accum) =>
      if (f(i)) i :: accum else accum
    }
  }

  /**
    * sumの定義を2つ用意しました。
    * 1つはscala.math.Numericを使ったもので（これは組み込みの機能を正確に再現しています）...。
    */
  import scala.math.Numeric

  def sumWithNumeric[A](list: List[A])(implicit numeric: Numeric[A]): A =
    list.foldRight(numeric.zero)(numeric.plus)

  sumWithNumeric(List(1, 2, 3))

  /**
    * cats.Monoidを使ったもの（本書の内容にはこちらの方がふさわしい）があります。
    */
  import cats.Monoid

  def sumWithMonoid[A](list: List[A])(implicit monoid: Monoid[A]): A =
    list.foldRight(monoid.empty)(monoid.combine)

  import cats.instances.int._ // for Monoid

  sumWithMonoid(List(1, 2, 3))
  // res13: Int = 6

  /**
    * 7.1.4 Foldable in Cats
    * CatsのFoldableは、foldLeftとfoldRightを抽象化して型クラスにしています。
    * Foldableのインスタンスは、これら2つのメソッドを定義し、多数の派生メソッドを継承します。
    * Catsでは、いくつかのScalaデータ型に対してFoldableのインスタンスをすぐに用意しています。
    * List、Vector、LazyList、Optionです。
    *
    * Foldable.applyを使って通常通りインスタンスを召喚し、そのインスタンスのfoldLeftの実装を直接呼び出すことができます。
    * ここでは、Listを使った例を示します。

     import cats.Foldable
     import cats.instances.list._ // for Foldable
     val ints = List(1, 2, 3)
     Foldable[List].foldLeft(ints, 0)(_ + _)

    * VectorやLazyListなどの他のシーケンスも同じように動作します。
    * ここでは、0または1の要素を持つシーケンスのように扱われるOptionを使った例を紹介します。
    *
     import cats.instances.option._ // for Foldable

     val maybeInt = Option(123)
     Foldable[Option].foldLeft(maybeInt, 10)(_ * _)
     // res1: Int = 1230

    * 7.1.4.1 Folding Right
    * Foldableは、Evalモナドの観点から、foldRightをfoldLeftとは異なる形で定義しています。

  def foldRight[A, B](fa: F[A], lb: Eval[B])(f: (A, Eval[B]) => Eval[B]): Eval[B]

    * Eval を使用すると、コレクションのデフォルトの foldRight の定義がそうでない場合でも、折りたたみが常にスタックセーフであることを意味します。
    * たとえば、LazyList に対する foldRight のデフォルトの実装は、スタックセーフではありません。
    * 遅延リストが長ければ長いほど、折り畳みのためのスタック要件が大きくなります。
    * 十分に大きい遅延リストは StackOverflowError を引き起こします。
    *
  import cats.Eval
  import cats.Foldable
  def bigData = (1 to 100000).to(LazyList)

  bigData.foldRight(0L)(_ + _)
  // java.lang.StackOverflowError ...
    *
    * Foldableを使うことで、スタックセーフな操作をすることになり、オーバーフローの例外が修正されました。
  val eval: Eval[Long] =
  Foldable[LazyList].foldRight(bigData, Eval.now(0L)) {
    (num, eval) => eval.map(_ + num)
  }


  eval.value
  // res3: Long = 5000050000L
    * Stack Safety in the Standard Library
    * 標準ライブラリを使用する場合、スタックセーフは通常問題になりません。
    * ListやVectorなど、よく使われるコレクション型には、スタックセーフなfoldRightの実装があります。
    *
   (1 to 100000).toList.foldRight(0L)(_ + _)
   // res4: Long = 5000050000L
   (1 to 100000).toVector.foldRight(0L)(_ + _)
   // res5: Long = 5000050000L
    *
    * ここでStreamを取り上げたのは、このルールの例外だからです。
    * しかし、どのようなデータタイプを使用していても、Evalが私たちをサポートしていることを知っておくと便利です。
    * ※Evalを使ったらStreamになるからこんなことを書いている？？
    *
    * 7.1.4.2 Folding with Monoids
    *
    * Foldableには、foldLeftの上で定義された多くの便利なメソッドが用意されています。
    * これらのメソッドの多くは、標準ライブラリのおなじみのメソッドの複製です。
    * find、exists、forall、toList、isEmpty、nonEmptyなどです。
    *
  Foldable[Option].nonEmpty(Option(42))
  // res6: Boolean = true

  Foldable[List].find(List(1, 2, 3))(_ % 2 == 0)
  // res7: Option[Int] = Some(2)
    *
    * これらのおなじみのメソッドに加えて、CatsにはMonoidsを利用した2つのメソッドが用意されています。
    * - combineAll（およびそのエイリアスであるfold）は、シーケンスのすべての要素をそのMonoidを使って結合します。
    * - foldMap は，ユーザが提供する関数をシーケンスにマッピングし，その結果を Monoid を用いて結合します．
    *
    * 例えば、List[Int]の総和を取るのにcombineAllを使うことができます。
    *
  import cats.instances.int._ // for Monoid

  Foldable[List].combineAll(List(1, 2, 3))
  // res8: Int = 6
    * 別の方法として、foldMapを使って各IntをStringに変換し、それらを連結することもできます。
    *
  import cats.instances.string._ // for Monoid

  Foldable[List].foldMap(List(1, 2, 3))(_.toString)
  // res9: String = "123"

    * 最後に、Foldableを組み合わせて、入れ子になった配列の深い探索をサポートすることができます。
  import cats.instances.vector._ // for Monoid

  val ints = List(Vector(1, 2, 3), Vector(4, 5, 6))

  (Foldable[List] compose Foldable[Vector]).combineAll(ints)
  // res11: Int = 21

    * 7.1.4.3 Syntax for Foldable
    *
    * Foldableのすべてのメソッドは、cats.syntax.foldable.Foldableを通じてシンタックス形式で利用できます。
    * いずれの場合も、Foldable上のメソッドの第一引数が、メソッド呼び出しの受信者となります。
    *
  import cats.syntax.foldable._ // for combineAll and foldMap

  List(1, 2, 3).combineAll
  // res12: Int = 6

  List(1, 2, 3).foldMap(_.toString)
  // res13: String = "123"
    *
    * Explicits over Implicits
    *
    * Scala は、そのメソッドがreciverで明示的に利用できない場合にのみ、Foldable のインスタンスを使用することを覚えておいてください。
    * 例えば、次のコードでは List で定義されているバージョンの foldLeft を使用します。
  List(1, 2, 3).foldLeft(0)(_ + _)
  // res14: Int = 6
    * 一方、以下の一般的なコードではFoldableを使用します。

  def sum[F[_]: Foldable](values: F[Int]): Int =
  values.foldLeft(0)(_ + _)

    * 私たちは通常、この区別を気にする必要はありません。
    * これは機能なのです。
    * 必要なメソッドを呼び出すと、コンパイラは必要に応じて Foldable を使用し、コードが期待通りに動作するようにします。
    * foldRight のスタックセーフな実装が必要な場合は、Eval をアキュムレータとして使用するだけで、コンパイラが Cats からメソッドを選択するようになります。
    *
    * 7.2 Traverse
    *
    * foldLeft や foldRight は柔軟な反復メソッドですが、アキュムレータやコンビネータ関数を定義するために多くの作業が必要となります。
    * Traverse 型クラスは、Applicatives を活用して、より便利で、より法則性のある反復のパターンを提供する上位のツールです。
    *
    * 7.2.1 Traversing with Futures
    *
    * Scalaの標準ライブラリにあるFuture.traverseとFuture.sequenceメソッドを使ってトラバースを実演することができます。
    * これらのメソッドは、TraverseパターンのFuture固有の実装を提供します。
    * 例として、サーバーのホスト名のリストと、ホストの稼働時間をポーリングするメソッドがあるとします。
    */
  import scala.concurrent._
  import scala.concurrent.ExecutionContext.Implicits.global

  val hostnames = List(
    "alpha.example.com",
    "beta.example.com",
    "gamma.demo.com"
  )

  def getUptime(hostname: String): Future[Int] =
    Future(hostname.length * 60) // just for demonstration

  /** ここで、すべてのホストを調査して、すべての稼働時間を収集したいとします。
    * ホスト名を単純にマッピングすることはできません。リスト[Future[Int]]の結果には複数のFutureが含まれているからです。
    * 結果を単一のFutureにして、ブロック化できるようにする必要があります。
    * まず、foldを使って手動でこれを行ってみましょう。
    val allUptimes: Future[List[Int]] =
      hostnames.foldLeft(Future(List.empty[Int])) {
        (accum, host) =>
          val uptime = getUptime(host)
          for {
            accum  <- accum
            uptime <- uptime
          } yield accum :+ uptime
      }
      Await.result(allUptimes, 1.second)
      // res0: List[Int] = List(1020, 960, 840)

    * 直感的には、ホスト名を反復処理し、各項目に対して func を呼び出し、結果をリストにまとめる。
    * これは簡単なように見えますが、繰り返しのたびにFutureを作成して組み合わせる必要があるため、コードはかなり扱いにくいものになっています。
    * このパターンに最適なFuture.traverseを使えば、大幅に改善することができます。
    *
    val allUptimes: Future[List[Int]] =
    Future.traverse(hostnames)(getUptime)

    Await.result(allUptimes, 1.second)
    // res2: List[Int] = List(1020, 960, 840)

    * こちらの方がはるかにわかりやすく、簡潔です。
    * CanBuildFromやExecutionContextのような邪魔なものを無視すると、標準ライブラリのFuture.traverseの実装は次のようになります。

    def traverse[A, B](values: List[A])
      (func: A => Future[B]): Future[List[B]] =
    values.foldLeft(Future(List.empty[B])) { (accum, host) =>
      val item = func(host)
      for {
        accum <- accum
        item  <- item
      } yield accum :+ item
    }

    * これは、上記のサンプルコードと基本的に同じです。
    * Future.traverseは、アキュムレータや組み合わせ関数を折り畳んだり定義したりする手間を抽象化しています。
    * これは、私たちが望むことを行うためのきれいな高レベルのインターフェイスを提供します。
    * - List[A]で始まる。
    * - 関数 A => Future[B] を提供します。
    * - 最終的にはFuture[List[B]]が生成されます。
    *
    * 標準ライブラリには、Future.sequenceという別のメソッドがあります。
    * これは、List[Future[B]]から始めることを前提としており、ID関数を用意する必要はありません。
    *
    object Future {
    def sequence[B](futures: List[Future[B]]): Future[List[B]] =
      traverse(futures)(identity)

    // etc...
  }
    * この場合、直感的な理解はさらにシンプルになります。
    * - List[Future[A]]から始まります。
    * - 最終的にはFuture[List[A]]となります。
    *　Future.traverseとFuture.sequenceは、非常に特殊な問題を解決します。
    * つまり、Futureのシーケンスを反復処理し、結果を蓄積することができます。
    * 上の簡単な例はリストでしか動作しませんが、実際のFuture.traverseとFuture.sequenceは標準的なScalaのコレクションで動作します。
    *
    * CatsのTraverse型クラスは、これらのパターンを一般化し、あらゆるタイプのApplicativeで動作するようにしています。
    * Future、Option、Validatedなどです。次のセクションでは、2つのステップでTraverseに取り組みます。
    * まずApplicativeを一般化し、次にシーケンスタイプを一般化します。
    * 最終的には、シーケンスや他のデータ型を含む多くの操作を簡略化する、非常に価値のあるツールになるでしょう。
    *
    * 7.2.2 Traversing with Applicatives
    *
    * 目を細めてみると、traverseをApplicativeの観点から書き換えることができることがわかります。
    * 上の例のアキュムレータです
    Future(List.empty[Int])
    * は、Applicative.pureと同等です。
    import cats.Applicative
    import cats.instances.future._   // for Applicative
    import cats.syntax.applicative._ // for pure

    List.empty[Int].pure[Future]

    * 私たちのコンビネーターは、以前はこれでした。
    def oldCombine(
      accum : Future[List[Int]],
      host  : String
    ): Future[List[Int]] = {
      val uptime = getUptime(host)
      for {
        accum  <- accum
        uptime <- uptime
      } yield accum :+ uptime
    }
    * はSemigroupal.combineと同等になりました。*/
  import cats.syntax.apply._ // for mapN

  // Combining accumulator and hostname using an Applicative:*/
  def newCombine(accum: Future[List[Int]], host: String): Future[List[Int]] =
    (accum, getUptime(host)).mapN(_ :+ _)
  /* これらの断片をtraverseの定義に戻すことで、任意のApplicativeで動作するように一般化することができます。*/
  def listTraverse[F[_]: Applicative, A, B](list: List[A])(
      func: A => F[B]): F[List[B]] =
    list.foldLeft(List.empty[B].pure[F]) { (accum, item) =>
      (accum, func(item)).mapN(_ :+ _)
    }

  def listSequence[F[_]: Applicative, B](list: List[F[B]]): F[List[B]] =
    listTraverse(list)(identity)

  /** listTraverseを使って、uptimeの例を再実装することができます。
    val totalUptime = listTraverse(hostnames)(getUptime)

    Await.result(totalUptime, 1.second)
    // res5: List[Int] = List(1020, 960, 840)
    * また、次の演習で示すように、他のApplicativeデータ型と組み合わせて使用することもできます。
    *
    * 7.2.2.1 Exercise: Traversing with Vectors
    *
    * 次の結果はどのようになりますか？*/
  import cats.instances.vector._ // for Applicative

  println(s"7.2.2.1_1 Exercise: Traversing with Vectors: ${listSequence(
    List(Vector(1, 2), Vector(3, 4)))}")
  // Vector(List(1, 3), List(1, 4), List(2, 3), List(2, 4))

  /**
    * 引数はList[Vector[Int]]型なので、VectorのApplicativeを使用し、戻り値の型はVector[List[Int]]になると思います。
    * Vectorはモナドなので、半群結合関数はflatMapをベースにしています。
    * 最終的には、List(1, 2)とList(3, 4)のすべての可能な組み合わせのListのVectorになります。
    // res7: Vector[List[Int]] = Vector(
    //   List(1, 3),
    //   List(1, 4),
    //   List(2, 3),
    //   List(2, 4)
    // )

    * 3つのパラメータのリストはどうでしょうか？
    * 入力リストに3つの項目がある場合、3つのIntsの組み合わせになります。
    * 1つ目の項目から1つ、2つ目の項目から1つ、3つ目の項目から1つです。
    */
  println(s"7.2.2.1_2 Exercise: Traversing with Vectors: ${listSequence(
    List(Vector(1, 2), Vector(3, 4), Vector(5, 6)))}")
  // Vector(List(1,3,5),List(1,3,6),List(2,3,5),List(2,3,6),List(2,4,5),List(2,4,6))

  /**
    * 7.2.2.2 Exercise: Traversing with Options
    *
    * Optionを使用して例はこれです。
    */
  import cats.instances.option._ // for Applicative

  def process(inputs: List[Int]): Option[List[Int]] = {
    listTraverse(inputs)(n => if (n % 2 == 0) Some(n) else None)
  }
  println(s"process_1: ${process(List(2, 4, 6))}")
  println(s"process_2: ${process(List(1, 2, 3))}")

  /**
    * このメソッドのリターンタイプは何ですか？次の入力に対して何を生成しますか？
    * listTraverseの引数はList[Int]とInt => Option[Int]の型なので、戻り値の型はOption[List[Int]]となります。
    * ここでも、Optionはモナドなので、半群結合関数はflatMapに従います。セマンティクスとしては、フェイルファストなエラー処理が可能で、すべての入力が偶数であれば、出力のリストが得られます。
    * そうでない場合は None を得る。
    * Some(List(2,4,6))
    * None
    *
    * 7.2.2.3 Exercise: Traversing with Validated
    *
    * 最後はValidatedの例です。
    */
  import cats.data.Validated
  import cats.instances.list._ // for Monoid

  type ErrorsOr[A] = Validated[List[String], A]

  def processValidated(inputs: List[Int]): ErrorsOr[List[Int]] =
    listTraverse(inputs) { n =>
      if (n % 2 == 0) {
        Validated.valid(n)
      } else {
        Validated.invalid(List(s"$n is not even"))
      }
    }

  /**
    * この方法では、以下の入力に対してどのような結果が得られるでしょうか。
    */
  println(s"process_1: ${processValidated(List(2, 4, 6))}")
  println(s"process_2: ${processValidated(List(1, 2, 3))}")
  //Validated(List(2, 4, 6))
  //Validated(List("1 is not even", "3 is not even"))

  // applicativeは蓄積型 Monadはfail first
  /**
    * 7.2.3 Traverse in Cats
    *
    * 当社の listTraverse および listSequence メソッドは、どのタイプの Applicative でも動作しますが、1 つのタイプのシーケンスでしか動作しません。
    * リストです。
    * 型クラスを使用して異なるシーケンスタイプを一般化することができますが、これが Cats' Traverse です。簡略化された定義は次のとおりです。
    */
  trait Traverse[F[_]] {
    def traverse[G[_]: Applicative, A, B](inputs: F[A])(
        func: A => G[B]): G[F[B]]

    def sequence[G[_]: Applicative, B](inputs: F[G[B]]): G[F[B]] =
      traverse(inputs)(identity)
  }

  /**
    * CatsはList、Vector、Stream、Option、Either、その他様々な型に対してTraverseのインスタンスを提供しています。
    * Traverse.applyを使って通常通りインスタンスを召喚し、前のセクションで説明したようにtraverseメソッドとsequenceメソッドを使用することができます。
    */

//  val totalUptime: Future[List[Int]] =
//    Traverse[List].traverse(hostnames)(getUptime)
//
//  Await.result(totalUptime, 1.second)
//  // res0: List[Int] = List(1020, 960, 840)
//
//  val numbers = List(Future(1), Future(2), Future(3))
//
//  val numbers2: Future[List[Int]] =
//    Traverse[List].sequence(numbers)
//
//  Await.result(numbers2, 1.second)
  // res1: List[Int] = List(1, 2, 3)

  /**
    * cats.syntax.traverseを介してインポートされた構文バージョンのメソッドもあります。
    */
//  import cats.syntax.traverse._ // for sequence and traverse
//
//  Await.result(hostnames.traverse(getUptime), 1.second)
//  // res2: List[Int] = List(1020, 960, 840)
//  Await.result(numbers.sequence, 1.second)
  // res3: List[Int] = List(1, 2, 3)

  /**
  * ご覧のように、この章の最初に紹介したfoldLeftのコードよりも、はるかにコンパクトで読みやすいものになっています。
  *
  * 7.3 Summary
  *
  * この章では、配列を反復処理するための 2 つの型クラスである Foldable と Traverse を紹介しました。
  *
  * Foldable は、標準ライブラリのコレクションでおなじみの foldLeft と foldRight のメソッドを抽象化したものです。
  * また、これらのメソッドのスタックセーフな実装を一握りの追加データ型に追加し、状況に応じて便利な追加機能を定義しています。
  * とはいえ、Foldable にはまだ知られていないことはあまりありません。
  *
  * 本当の力を発揮するのはTraverseで、Futureでおなじみのtraverseとsequenceのメソッドを抽象化して一般化しています。
  * これらのメソッドを使うと、Traverseのインスタンスを持つ任意のFとApplicativeのインスタンスを持つ任意のGに対して、F[G[A]]をG[F[A]]に変えることができます。
  * コードの行数を減らすという意味では、Traverseはこの本の中でも最も強力なパターンの一つです。
  * 何行にもわたる折り返しを、たったひとつの foo.traverse にまで減らすことができます。
  *
  * ...ということで、この本では理論的な説明をすべて終えました。
  * 第2部では、学んだことを実践するためのケーススタディが用意されています。
  */

}
