//package cats.Chapter6
//
//import cats.implicits.catsSyntaxTuple2Parallel
//import cats.{Functor, ~>}
//import cats.implicits._
//
//object SemigroupalAndApplicative extends App {
//
//  /**
//    * 6 Semigroupal and Applicative
//    * ファンクタとモナドはどちらも非常に便利な抽象概念ですが、プログラムの流れを表現できないタイプもあります。
//    *
//    *
//  その一つがフォームの検証です。
//  フォームを検証する際には、最初に発生したエラーで停止するのではなく、すべてのエラーをユーザーに返したいと考えています。
//  これをEitherのようなモナドでモデル化すると、早く失敗してエラーがなくなってしまいます。
//  例えば、以下のコードは、parseIntの最初の呼び出しで失敗し、それ以上は実行されません。
//    */
//  import cats.syntax.either._ // for catchOnly
//
//  def parseInt(str: String): Either[String, Int] =
//    Either
//      .catchOnly[NumberFormatException](str.toInt)
//      .leftMap(_ => s"Couldn't read $str")
//
//  for {
//    a <- parseInt("a")
//    b <- parseInt("b")
//    c <- parseInt("c")
//  } yield (a + b + c)
//  // res0: Either[String, Int] = Left("Couldn't read a")
//
//  /**
//    * もうひとつの例は、Futuresの同時評価です。
//    * 複数の独立したタスクが長時間実行される場合、それらを同時に実行することは理にかなっています。
//    * mapとflatMapは、それぞれの計算が前の計算に依存しているという仮定をしているので、私たちが望んでいることを理解することができません。
//    */
//  // context2 is dependent on value1:
////  context1.flatMap(value1 => context2)
//  /**
//    * 上記のparseIntとFuture.applyの呼び出しは互いに独立していますが、mapとflatMapはこれを利用することができません。
//    * 求める結果を得るためには、順序付けを保証しない、より弱い構造が必要です。
//    * この章では、このパターンをサポートする3つの型クラスを紹介します。
//    *
//    * - Semigroupalは、コンテキストのペアを構成する概念を包含しています。
//    * Catsでは、半群化とFunctorを利用して、複数の引数を持つ関数を並べることができるcats.syntax.applyモジュールを提供しています。
//    *
//    * - Parallelは、Monadインスタンスを持つ型をSemigroupalインスタンスを持つ関連型に変換します。
//    *
//    * - ApplicativeはSemigroupalとFunctorを継承しています。
//    * これは、コンテキスト内のパラメータに関数を適用する方法を提供します。
//    * Applicativeは、第4章で紹介したピュアメソッドの元になっています。
//    *
//    * Applicativeは、Catsで強調されているsemigroupな定式化ではなく、関数の適用という観点から定式化されることがよくあります。
//    * この代替的な定式化は、ScalazやHaskellなどの他のライブラリや言語へのリンクとなっています。
//    * Applicativeのさまざまな定式化や、Semigroupal、Functor、Applicative、Monadの関係については、本章の最後の方で見ていきます。
//    *
//    * 6.1 Semigroupal
//    *
//    * cats.Semigroupalは、contexts9を組み合わせることができる型クラスです。
//    * F[A]とF[B]という2つの型のオブジェクトがある場合、Semigroupal[F]を使えば、それらを組み合わせてF[(A, B)]を作ることができます。
//    * Catsでの定義は
//    */
//  trait Semigroupal[F[_]] {
//    def product[A, B](fa: F[A], fb: F[B]): F[(A, B)]
//  }
//
//  /**
//    * 本章の冒頭で説明したように，パラメータ fa と fb は互いに独立しており，どちらの順序で計算しても product に渡すことができます．
//    * これは、パラメータに厳格な順序を課しているflatMapとは対照的です。
//    * このため、Semigroupalのインスタンスを定義する際には、Monadsを定義するときよりも自由度が高くなります。
//    *
//    * 6.1.1 Joining Two Contexts
//    *
//    * Semigroupでは値を結合することができますが、Semigroupalではコンテクストを結合することができます。
//    * 例として、いくつかのOptionsを結合してみましょう。
//    */
//  import cats.Semigroupal
//  import cats.instances.option._ // for Semigroupal
//
//  Semigroupal[Option].product(Some(123), Some("abc"))
//  // res1: Option[(Int, String)] = Some((123, "abc"))
//
//  /**
//    * 両方のパラメータがSomeのインスタンスである場合、その中の値のタプルが得られます。
//    * どちらかのパラメータがNoneと評価された場合、結果全体がNoneとなります。
//    */
//  Semigroupal[Option].product(None, Some("abc"))
//  // res2: Option[Tuple2[Nothing, String]] = None
//  Semigroupal[Option].product(Some(123), None)
//  // res3: Option[Tuple2[Int, Nothing]] = None
//  /**
//    * 6.1.2 Joining Three or More Contexts
//    *
//    * Semigroupalのコンパニオン・オブジェクトでは、productの上にメソッドのセットを定義しています。
//    * たとえば、tuple2からtuple22までのメソッドは、productをさまざまなアーリティに一般化します。
//    */
//  import cats.instances.option._ // for Semigroupal
//
//  Semigroupal.tuple3(Option(1), Option(2), Option(3))
//  // res4: Option[(Int, Int, Int)] = Some((1, 2, 3))
//  Semigroupal.tuple3(Option(1), Option(2), Option.empty[Int])
//  // res5: Option[(Int, Int, Int)] = None
//
//  /**
//    * メソッド map2 ～ map22 は、2 ～ 22 のコンテキスト内の値に、ユーザーが指定した関数を適用します。
//    */
//  Semigroupal.map3(Option(1), Option(2), Option(3))(_ + _ + _)
//  // res6: Option[Int] = Some(6)
//
//  Semigroupal.map2(Option(1), Option.empty[Int])(_ + _)
//  // res7: Option[Int] = None
//  /**
//    * また、contramap2～contramap22、imap2～imap22というメソッドがあり、それぞれContravariantとInvariantのインスタンスを必要とします。
//    *
//    * 6.1.3 Semigroupal Laws
//    *
//    * Semigroupalには1つだけ法則があります：積法は連想的でなければなりません。
//    */
////  product(a, product(b, c)) == product(product(a, b), c)
//
//  /**
//    * 6.2 Apply Syntax
//    *
//    * Catsには便利なapply構文が用意されており、上述のメソッドの略記法を提供しています。
//    * この構文は cats.syntax.apply からインポートします。以下はその例です。
//    */
//  import cats.instances.option._ // for Semigroupal
//  import cats.syntax.apply._ // for tupled and mapN
//  /**
//    * tupledメソッドは、オプションのタプルに暗黙のうちに追加されます。
//    * これは、Semigroupal for Optionを使用して、Optionの内部の値をzipで圧縮し、タプルの単一のOptionを作成します。
//    */
////  (Option(123), Option("abc")).tupled
//  // res8: Option[(Int, String)] = Some((123, "abc"))
//
//  /**
//    * 22個までの値を持つタプルにも同じトリックを使うことができます。
//    * Catsでは、各アリティに対して個別のタプルメソッドを定義しています。
//    */
////  (Option(123), Option("abc"), Option(true)).tupled
//  // res9: Option[(Int, String, Boolean)] = Some((123, "abc", true))
//  /**
//    * Catsのapply構文には、tupledの他に、mapNというメソッドがあります。
//    * これは、暗黙のFunctorと、その値を結合する正しいアリティの関数を受け取ります。
//    */
////  final case class Cat(name: String, born: Int, color: String) {
////
////    (
////      Option("Garfield"),
////      Option(1978),
////      Option("Orange & black")
////    ).mapN(Cat.apply)
////
////  }
//  // res10: Option[Cat] = Some(Cat("Garfield", 1978, "Orange & black"))
//
//  /**
//    * ここで紹介した方法の中では、mapNを使うのが最も一般的です。
//    *
//    * 内部的にはmapNはOptionから値を抽出するためにSemigroupalを使い、その値を関数に適用するためにFunctorを使います。
//    *
//    * この構文が型チェックされているのは嬉しいですね。
//    * 間違った数やタイプのパラメータを受け入れる関数を供給すると、コンパイルエラーが発生します。
//    */
////  val add: (Int, Int) => Int = (a, b) => a + b
//  // add: (Int, Int) => Int = <function2>
//
////  (Option(1), Option(2), Option(3)).mapN(add)
//  // error: ':' expected but '(' found.
//  //   Option("Garfield"),
//  //         ^
//  // error: identifier expected but '}' found.
//
////  (Option("cats"), Option(true)).mapN(add)
//  // error: ':' expected but '(' found.
//  //   Option("Garfield"),
//  //         ^
//  // error: identifier expected but '}' found.
//
//  /**
//    * 6.2.1 Fancy Functors and Apply Syntax
//    *
//    * Apply構文には、ContravariantとInvariantのファンクタを受け入れるcontramapNとimapNメソッドもあります。
//    * 例えば、Invariant を使って Monoids を組み合わせることができます。
//    * 以下にその例を示します。
//    */
//  import cats.Monoid
//  import cats.instances.int._ // for Monoid
//  import cats.instances.invariant._ // for Semigroupal
//  import cats.instances.list._ // for Monoid
//  import cats.instances.string._ // for Monoid
//  import cats.syntax.apply._ // for imapN
//
////  final case class Cat(
////      name: String,
////      yearOfBirth: Int,
////      favoriteFoods: List[String]
////  )
//
//  val tupleToCat: (String, Int, List[String]) => Cat =
//    Cat.apply _
//
//  val catToTuple: Cat => (String, Int, List[String]) =
//    cat => (cat.name, cat.yearOfBirth, cat.favoriteFoods)
//
//  implicit val catMonoid: Monoid[Cat] = (
//    Monoid[String],
//    Monoid[Int],
//    Monoid[List[String]]
//  ).imapN(tupleToCat)(catToTuple)
//
//  /**
//    * このMonoidでは、「空」のCatsを作成したり、第2章の構文を使ってCatsを追加したりすることができます。
//    */
//  import cats.syntax.semigroup._ // for |+|
//
////  val garfield = Cat("Garfield", 1978, List("Lasagne"))
////  val heathcliff = Cat("Heathcliff", 1988, List("Junk Food"))
//
////  garfield |+| heathcliff
//  // res14: Cat = Cat("GarfieldHeathcliff", 3966, List("Lasagne", "Junk Food"))
//
//  /**
//    * 6.3 Semigroupal Applied to Different Types
//    *
//    * Semigroupalは、特にMonadのインスタンスを持つ型に対しては、必ずしも期待通りの動作を提供するとは限りません。
//    * これまでOptionのの挙動を見てきました。
//    * 他の型の例を見てみましょう。
//    *
//    * Future
//    *
//    * Futureのセマンティクスは、逐次実行ではなく、並列実行を可能にします。
//    */
//  import cats.Semigroupal
//  import cats.instances.future._ // for Semigroupal
//  import scala.concurrent._
//  import scala.concurrent.duration._
//  import scala.concurrent.ExecutionContext.Implicits.global
//
//  val futurePair = Semigroupal[Future].product(Future("Hello"), Future(123))
//
//  Await.result(futurePair, 1.second)
//  // res0: (String, Int) = ("Hello", 123)
//
//  /**
//    * 2つのFuturesは、作成した瞬間から実行されるので、productを呼び出すまでにすでに結果を計算しています。
//    * apply構文を使って、固定数のFuturesをZIPすることができます。
//    */
//  import cats.syntax.apply._ // for mapN
//
//  case class Cat(
//      name: String,
//      yearOfBirth: Int,
//      favoriteFoods: List[String]
//  )
//
//  val futureCat = (
//    Future("Garfield"),
//    Future(1978),
//    Future(List("Lasagne"))
//  ).mapN(Cat.apply)
//
//  Await.result(futureCat, 1.second)
//  // res1: Cat = Cat("Garfield", 1978, List("Lasagne"))
//
//  /**
//    * List
//    *
//    * ListsとSemigroupalを組み合わせることで、予想外の結果が得られます。
//    * 次のようなコードでリストを圧縮することを期待していましたが、実際にはリストの要素のカルテシアン積を得ることができました。
//    */
//  import cats.Semigroupal
//  import cats.instances.list._ // for Semigroupal
//
//  Semigroupal[List].product(List(1, 2), List(3, 4))
//  // res2: List[(Int, Int)] = List((1, 3), (1, 4), (2, 3), (2, 4))
//
//  /**
//    * これは意外と知られていません。
//    * リストをジップするのは、もっと一般的な操作です。
//    * なぜこのような動作になるのかは、後ほど説明します。
//    *
//    * Either
//    *
//    * この章の冒頭で、フェイルファーストとアキュムレーションのエラー処理について説明しました。
//    * Eitherに適用されたproductは、フェイルファーストではなく、エラーを蓄積すると予想されます。
//    * 意外かもしれませんが、productはflatMapと同じようにフェイルファーストを実装しています。
//    */
//  import cats.instances.either._ // for Semigroupal
//
////  type ErrorOr[A] = Either[Vector[String], A]
//
//  Semigroupal[ErrorOr].product(
//    Left(Vector("Error 1")),
//    Left(Vector("Error 2"))
//  )
//  // res3: ErrorOr[Tuple2[Nothing, Nothing]] = Left(Vector("Error 1"))
//
//  /**
//    * この例では、製品は最初の失敗を見て停止しますが、2番目のパラメータを調べて、それも失敗であることを確認することは可能です。
//    *
//    * 6.3.1 Semigroupal Applied to Monads
//    *
//    * ListとEitherが意外な結果になるのは、どちらもモナドだからです。
//    * もしモナドがあれば、次のようにしてproductを実装できます。
//    */
//  import cats.Monad
//  import cats.syntax.functor._ // for map
//  import cats.syntax.flatMap._ // for flatmap
//
//  def product[F[_]: Monad, A, B](fa: F[A], fb: F[B]): F[(A, B)] =
//    fa.flatMap(a => fb.map(b => (a, b)))
//
//  /**
//    * もし、実装方法によってproductの意味が違っていたら、とても奇妙なことです。
//    * 一貫したセマンティクスを確保するために、（Semigroupalを拡張した）Cats'Monadは、上で示したように、mapとflatMapの観点からproductの標準的な定義を提供しています。
//    *
//    * Futureの結果も、光のトリックです。
//    * flatMapは順序付けを行うので、productも同じように順序付けを行います。
//    * 私たちが観察する並列実行は、構成するFutureがproductを呼び出す前に実行を開始するために起こります。
//    * これは、古典的なcreate-then-flatMapパターンと同じです。
//    */
//  val a = Future("Future 1")
//  val b = Future("Future 2")
//
//  for {
//    x <- a
//    y <- b
//  } yield (x, y)
//
//  /**
//    * では、なぜSemigroupalをわざわざ使うのでしょうか？
//    * その答えは、MonadではなくSemigroupal（とApplicative）のインスタンスを持つ便利なデータ型を作ることができるからです。
//    * これにより、様々な方法でproductを実装することができます。この点については、後ほどエラー処理のための代替データ型を見たときに詳しく説明します。
//    *
//    * 6.3.1.1 Exercise: The Product of Lists
//    *
//    * ListのproductはなぜCartesian productになるのか？
//    * 上で例を見ました。ここでもう一度。
//    */
//  Semigroupal[List].product(List(1, 2), List(3, 4))
//  // res5: List[(Int, Int)] = List((1, 3), (1, 4), (2, 3), (2, 4))
//  /**
//    * これをtupledで書くこともできます。
//    */
//  (List(1, 2), List(3, 4)).tupled
//  // res6: List[(Int, Int)] = List((1, 3), (1, 4), (2, 3), (2, 4))
//
//  // Exercise
//
//  import cats.syntax.functor._ // for map
//  import cats.syntax.flatMap._ // for flatMap
//
//  /**
//    * この演習では、フラットマップとマップの観点から製品の定義を理解したかどうかを確認します。
//    */
//  def product[F[_]: Monad, A, B](x: F[A], y: F[B]): F[(A, B)] =
//    x.flatMap(a => y.map(b => (a, b)))
//
//  /**
//    * このコードは、for comprehensionに相当します。
//    */
//  def product[F[_]: Monad, A, B](x: F[A], y: F[B]): F[(A, B)] = {
//    for {
//      a <- x
//      b <- y
//    } yield (a, b)
//
//    /**
//      * フラットマップのセマンティクスは、ListとEitherの動作を生み出しています。
//      */
//    product(List(1, 2), List(3, 4))
//    // res9: List[(Int, Int)] = List((1, 3), (1, 4), (2, 3), (2, 4))
//  }
//
//  /**
//    * 6.4 Parallel
//    * 前のセクションでは、モナドのインスタンスを持つ型に対してproductを呼び出すと、シーケンシャルなセマンティクスが得られることを説明しました。
//    * これは、flatMapやmapを使ったproductの実装との整合性を保つという観点からは意味があります。
//    * しかし、これは必ずしも私たちが望むものではありません。
//    * パラレル型クラスとそれに関連するシンタックスにより、特定のモナドの代替セマンティクスにアクセスすることができます。
//    *
//    * Eitherのプロダクトメソッドが最初のエラーで止まってしまうことを見てきました。
//    */
//  import cats.Semigroupal
//  import cats.instances.either._ // for Semigroupal
//
//  type ErrorOr[A] = Either[Vector[String], A]
//  val error1: ErrorOr[Int] = Left(Vector("Error 1"))
//  val error2: ErrorOr[Int] = Left(Vector("Error 2"))
//
//  Semigroupal[ErrorOr].product(error1, error2)
//  // res0: ErrorOr[(Int, Int)] = Left(Vector("Error 1"))
//  /**
//    * これはtupledを使ってショートカットで書くこともできます。
//    */
//  import cats.syntax.apply._ // for tupled
//  import cats.instances.vector._ // for Semigroup on Vector
//
//  (error1, error2).tupled
//  // res1: ErrorOr[(Int, Int)] = Left(Vector("Error 1"))
//
//  /**
//    *
//  すべてのエラーを収集するには、tupledをparTupledという「並列」バージョンに置き換えるだけです。
//    */
////  import cats.syntax.parallel._ // for parTupled
//
////  (error1, error2).parTupled
//  // res2: ErrorOr[(Int, Int)] = Left(Vector("Error 1", "Error 2"))
//
//  /**
//    * 両方のエラーが返されることに注目してください。
//    * この動作は、エラータイプとしてVectorを使用する場合に限ったことではありません。
//    * Semigroupインスタンスを持つどのような型でも動作します。
//    * 例えば、ここでは代わりにListを使用しています。
//    */
//  import cats.instances.list._ // for Semigroup on List
//
//  type ErrorOrList[A] = Either[List[String], A]
//  val errStr1: ErrorOrList[Int] = Left(List("error 1"))
//  val errStr2: ErrorOrList[Int] = Left(List("error 2"))
//
//  (errStr1, errStr2).parTupled
//  // res3: ErrorOrList[(Int, Int)] = Left(List("error 1", "error 2"))
//  /**
//    * Semigroupalや関連する型のメソッドに対してParallelが提供する構文メソッドは数多くありますが、最もよく使われるのがparMapNです。
//    * ここでは、エラー処理の場面でのparMapNの例を紹介します。
//    */
//  val success1: ErrorOr[Int] = Right(1)
//  val success2: ErrorOr[Int] = Right(2)
//  val addTwo = (x: Int, y: Int) => x + y
//
////  (error1, error2).parMapN(addTwo)
//  // res4: ErrorOr[Int] = Left(Vector("Error 1", "Error 2"))
////  (success1, success2).parMapN(addTwo)
//  // res5: ErrorOr[Int] = Right(3)
//
//  /**
//    * それでは、Parallelの仕組みをご紹介します。以下の定義がParallelの核となる部分です。
//    */
//  trait Parallel[M[_]] {
//    type F[_]
//
//    def applicative: Applicative[F]
//    def monad: Monad[M]
//    def parallel: ~>[M, F]
//  }
//
//  /**
//    * これは、ある型コンストラクタMのParallelインスタンスが存在するならば、それを教えてくれます。
//    *
//    * - Mにはモナドのインスタンスがあるはずです。
//    * - Applicativeインスタンスを持つ関連型コンストラクタFがあること。
//    * - MをFに変換することができます。
//    *
//    * これまでに ~> を見たことはありません。
//    * これはFunctionKの型の別名で、MからFへの変換を行うものです。
//    * 通常の関数A => BはA型の値をB型の値に変換します。MとFは型ではなく、型構成子であることを覚えておいてください。
//    * FunctionK M ~> Fは、M[A]という型の値からF[A]という型の値への関数です。
//    * それでは、OptionをListに変換するFunctionKを定義して、簡単な例を見てみましょう。
//    *
//    *
//    */
//  import cats.arrow.FunctionK
//
//  object optionToList extends FunctionK[Option, List] {
//    def apply[A](fa: Option[A]): List[A] =
//      fa match {
//        case None    => List.empty[A]
//        case Some(a) => List(a)
//      }
//  }
//
//  optionToList(Some(1))
//  // res6: List[Int] = List(1)
//  optionToList(None)
//  // res7: List[Nothing] = List()
//
//  /**
//    * 型パラメータAがジェネリックであるため、FunctionKは型コンストラクタMに含まれる値を検査することができません。
//    * 変換は純粋に型コンストラクタMとFの構造に基づいて行う必要があります。
//    *
//    * 要約すると，Parallelを使うと，モナドインスタンスを持つ型を，代わりに応用型（または半群型）インスタンスを持つ何らかの関連型に変換することができます．
//    * この関連型はいくつかの便利な代替セマンティクスを持ちます。
//    * 上の例では、Eitherの関連する応用型が、フェイルファーストのセマンティクスではなく、エラーの蓄積を可能にしていました。
//    *
//    * Parallelを見てきたので、いよいよApplicativeについて学びましょう。
//    *
//    * 6.4.0.1 練習問題。並列リスト
//    *
//    * ListにはParallelインスタンスがありますか？あるとしたら、Parallelインスタンスは何をしますか？
//    **/
//  import cats.instances.list._
//
//  (List(1, 2), List(3, 4)).tupled
//  // res8: List[(Int, Int)] = List((1, 3), (1, 4), (2, 3), (2, 4))
//  (List(1, 2), List(3, 4)).parTupled
//  // res9: List[(Int, Int)] = List((1, 3), (2, 4))
//  /**
//    * ListはParallelインスタンスを持っていて、 cartesian productを作成する代わりにListをzipします。
//    * // prallelがあったらMonadだけどApplicativeのように使える
//    * 6.5 Apply and Applicative
//    *
//    * 半群関数は、関数型プログラミングの文献ではあまり言及されていません。
//    * 半群化は、応用ファンクタ（略して「応用」）と呼ばれる関連する型クラスの機能のサブセットを提供します。
//    *
//    * 半群関数と応用関数は、コンテキストの結合という同じ概念の別のエンコーディングを効果的に提供します。
//    * 両者は2008年にConor McBrideとRoss Patersonが発表した論文で紹介されています。
//    *
//    * Catsは2つの型クラスを使って適用語をモデル化しています。1つ目の cats.ApplyはSemigroupalとFunctorを継承し、コンテキスト内の関数にパラメータを適用するapメソッドを追加しています。
//    * 2つ目の cats.Applicative は Apply を拡張し、第4章で紹介した pure メソッドを追加しています。
//    * 簡略化した定義をコードで示します。
//    */
//  trait Apply[F[_]] extends Semigroupal[F] with Functor[F] {
//    def ap[A, B](ff: F[A => B])(fa: F[A]): F[B]
//
//    def product[A, B](fa: F[A], fb: F[B]): F[(A, B)] =
//      ap(map(fa)(a => (b: B) => (a, b)))(fb)
//  }
//
//  trait Applicative[F[_]] extends Apply[F] {
//    def pure[A](a: A): F[A]
//  }
//
//  /**
//  * これを分解すると、apメソッドは、コンテキストF[_]内の関数ffにパラメータfaを適用します。
//  * Semigroupalのproductメソッドは、apとmapで定義されています。
//  *
//  * productの実装については、あまり気にしないでください。
//  * 重要なのは、product、ap、mapの間には緊密な関係があり、これらのうちのどれか1つを他の2つの項で定義できるということです。
//  *
//  * Applicativeにはpureメソッドもあります。これは、Monadで見たのと同じpureです。
//  * このメソッドは、ラップされていない値から新しいApplicativeインスタンスを構築します。
//  * この意味で、MonoidがSemigroupに関連しているように、ApplicativeはApplyに関連しています。
//  *
//  * 6.5.1 The Hierarchy of Sequencing Type Classes
//  *
//  * ApplyとApplicativeが導入されたことで、計算をさまざまな方法で実行するための型クラスの全体像が見えてきました。
//  * 図10は、この本で取り上げた型クラスの関係を示しています。
//  *
//  * == 図10 ==
//  *
//  * 階層内の各型クラスは、特定のシーケンスセマンティクスのセットを表し、特徴的なメソッドのセットを導入し、それらの観点からそのスーパータイプの機能を定義します。
//  * - すべてのモナドはアプリケ-ションである。
//  * - すべてのアプリケ-ションはセミグル-プである。といった具合です。
//  *
//  * 型クラス間の関係には法則性があるため、継承関係は型クラスのすべてのインスタンスで一定です。
//  * Applyは、apとmapでproductを定義し、Monadは、pureとflatMapでproduct、ap、mapを定義しています。
//  *
//  * これを説明するために、2つの仮想的なデータ型を考えてみましょう。
//  *
//  * Fooはモナドです。
//  * pureとflatMapを実装し、product、map、apの標準的な定義を継承するモナド型クラスのインスタンスを持っています。
//  *
//  * Barは、アプリケーティブ・ファンクタです。
//  * これは、pureとapを実装し、productとmapの標準的な定義を継承しているApplicativeのインスタンスを持っています。
//  *
//  * この2つのデータ型について、実装の詳細を知らずに何か言えることはありますか？
//  *
//  * FooについてはBarよりも厳密に知っています。MonadはApplicativeのサブタイプなので、Barでは保証できないFooのプロパティ（flatMap）を保証できます。
//  * 逆に、BarはFooよりも広い範囲で動作することがわかっています。
//  * 従うべき法則が少ない（flatMapがない）ので、Fooにはできない動作を実装することができます。
//  *
//  * これは、（数学的な意味での）パワーと制約の古典的なトレードオフを示しています。
//  * データ型に制約を加えれば加えるほど、そのデータ型の動作を保証することができますが、モデル化できる動作は少なくなります。
//  *
//  * モナドは、このトレードオフの中で、ちょうど良い位置にあります。
//  * 幅広い動作をモデル化するのに十分な柔軟性と、それらの動作について強い保証を与えるのに十分な制限性を備えています。
//  * しかし、モナドが仕事に適したツールでない場合もあります。
//  * 例えば、タイ料理が食べたくて、ブリトーでは満足できないことがあります。
//  *
//  * 単項式はモデルとなる計算に厳密な順序を課しますが、応用式や半群式にはそのような制限はありません。
//  * そのため、階層の中では異なる場所に位置しています。
//  * 単項式では表現できないような、並列／独立した計算のクラスを表現するために使用することができます。
//  *
//  * データ構造を選択することでセマンティクスを選択します。モナドを選択すれば、厳密な順序付けができます。
//  * アプリケーショ ンを選択すると、フラットマップの能力を失います。
//  * これは、一貫性の法則によるトレードオフです。ですから、型の選択は慎重に行ってください。
//  *
//  * 6.6 Summary
//  *
//  * 本書で取り上げた配列データ型の中で最も広く使われているのはモナドとファンクタですが、最も一般的なのは半群とアプリケ-ションです。
//  * これらの型クラスは、文脈の中で値を結合したり関数を適用したりするための汎用的なメカニズムを提供し、そこからモナドや他のさまざまな結合子を作り出すことができます。
//  *
//  * 半群型と応用型は、検証ルールの結果のような独立した値を組み合わせる手段として最もよく使われます。
//  * Catsはこの目的のためにValidated型を提供し、ルールの組み合わせを表現する便利な方法としてapply構文を提供しています。
//  *
//  * これで、本書の課題である関数型プログラミングの概念をほぼすべて網羅しました。
//  * 次の章では、データ型間の変換を行う強力な型クラスであるTraverseとFoldableを取り上げます。
//  * その後、第1部の概念をすべてまとめたいくつかのケーススタディを見ていきます。
//  */
//
//}
