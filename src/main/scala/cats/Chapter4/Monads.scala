package cats.Chapter4

object Monads extends App {

  /**
    * モナドはScalaで最も一般的な抽象化の一つです。Scalaのプログラマの多くは、名前を知らなくてもすぐに直観的にモナドに親しむことができます。
    * 非公式には、モナドとはコンストラクタとflatMapメソッドを持つものを指します。
    * 前章で見たOption、List、Futureなどのファンクタもすべてモナドです。
    * モナドをサポートするための特別な構文もあります。
    * しかし、この概念が一般的に普及しているにもかかわらず、Scala標準ライブラリには「flatMappedできるもの」を包含する具体的な型が欠けています。
    * この型クラスは、Catsによってもたらされた利点の一つです。この章では、モナドについて深く掘り下げていきます。
    * まず、いくつかの例を挙げてモナドのモチベーションを高めることから始めます。次に、モナドの正式な定義とCatsでの実装について説明します。
    * 最後に、あなたが見たことがないかもしれない興味深いモナドを紹介し、その使用例を提供します。
    */
  // 4.1 What is a Monad?
  /**
    * これは、cats、メキシコ料理、有毒廃棄物でいっぱいの宇宙服、エンドファンクター（それが何を意味するのかは知らないが）のカテゴリーにあるモノイドなど、
    * 多様な概念を含む説明や類推で、1000回ものブログ記事で提起されてきた問題である。
    * 我々は、非常に簡単に以下のように述べることで、モナドを説明するという問題を一度に解決しようとしています。
    */
  // モナドは、シーケンシング計算のためのメカニズムです。

  /**
    * 簡単です！問題は解決しましたね？前の章では、ファンクタは全く同じことをするための制御機構だと言っていました。
    * もうちょっと議論が必要ですね...第3.1節では、ファンクタを使うと複雑さを無視して計算を連続させることができると述べました。
    * しかし、ファンクタはこの複雑さをシーケンスの最初に一度だけ発生させるという点で制限があります。
    * ファンクタはシーケンスの各ステップでの複雑さを考慮していません。そこでモナドの出番です。
    * モナドのflatMapメソッドは、中間的な複雑さを考慮して次に何が起こるかを指定することができます。
    * Option の flatMap メソッドは、中間的な Options を考慮に入れています。ListのflatMapメソッドは、中間のListを処理します。
    * といった具合です。
    * いずれの場合も、flatMap に渡された関数はアプリケーション固有の計算部分を指定し、flatMap 自体が複雑な処理を行い、再び flatMap を実行できるようにしています。
    * いくつかの例を見てみましょう。
    */
  // Options

  /**
    * オプションを使用すると、値を返しても返さなくてもよい計算を連続して行うことができます。
    * 以下にいくつかの例を示します。
    */
  def parseInt(str: String): Option[Int] =
    scala.util.Try(str.toInt).toOption

  def divide(a: Int, b: Int): Option[Int] =
    if (b == 0) None else Some(a / b)

  /**
    * これらのメソッドはそれぞれ None を返すことで "失敗" することがあります。
    * flatMap メソッドでは、操作をシーケンスする際にこれを無視することができます。
    */
  //  def stringDivideBy(aStr: String, bStr: String): Option[Int] =
  //    parseInt(aStr).flatMap { aNum =>
  //      parseInt(bStr).flatMap { bNum =>
  //        divide(aNum, bNum)
  //      }
  //    }

  /**
    * セマンティクスはこうです。
    *
    * - parseInt の最初の呼び出しは None または Some を返します。
    * - もしそれがSomeを返すならば、flatMapメソッドは関数を呼び出して整数aNumを渡します。
    * - parseInt の 2 回目の呼び出しは None または Some を返します。
    * - もしそれがSomeを返すならば、flatMapメソッドは関数を呼び出してbNumを渡します。
    * - divide の呼び出しは None または Some を返し、これが結果となります。
    *
    * 各ステップで、flatMap は関数を呼び出すかどうかを選択し、関数が次の計算を生成します。
    * これを図 8 に示します．
    */
  // == 図8 ==

  /**
    * 計算の結果は Option であり、flatMap を再度呼び出すことができます。
    * これにより、おなじみのフェールファストエラー処理の振る舞いが実現します。
    */
  stringDivideBy("6", "2")
  // res0: Option[Int] = Some(3)
  stringDivideBy("6", "0")
  // res1: Option[Int] = None
  stringDivideBy("6", "foo")
  // res2: Option[Int] = None
  stringDivideBy("bar", "2")
  // res3: Option[Int] = None

  /**
    * すべてのモナドはファンクタでもあるので（証明については以下を参照）、新しいモナドを導入しても導入しなくても、flatMapとmapの両方を頼りにしてシーケンス計算を行うことができます。
    * さらに、flatMapとmapの両方を持っていれば、理解のためにシーケンスの振る舞いを明確にするために使うことができます。
    */
  def stringDivideBy(aStr: String, bStr: String): Option[Int] =
    for {
      aNum <- parseInt(aStr)
      bNum <- parseInt(bStr)
      ans <- divide(aNum, bNum)
    } yield ans

  // Lists
  /**
    * Scala の開発者になって初めて flatMap に出会ったとき、私たちはそれをリストの反復処理のパターンとして考えがちです。
    * これは for 内包の構文によって補強されており、ループのための命令的な構文に非常に似ています。
    */
  for {
    x <- (1 to 3).toList
    y <- (4 to 5).toList
  } yield (x, y)
  // res5: List[(Int, Int)] = List(
  //   (1, 4),
  //   (1, 5),
  //   (2, 4),
  //   (2, 5),
  //   (3, 4),
  //   (3, 5)
  // )
  (1 to 3).toList.flatMap(x => (4 to 5).map(y => (x, y)))

  /**
    * しかし、リストの単項的な振る舞いを浮き彫りにするもう一つのメンタルモデルがあります。
    * リストを中間結果の集合として考えると、flatMapは順列や組み合わせを計算する構成要素になります。
    *
    * 例えば、上の理解度の例では、3つの可能なxの値と2つの可能なyの値があります。
    * これは、(x, y)には6つの可能な値があることを意味します。
    *
    * - xを取得
    * - yを取得
    * - (x,y)のタプルを作成
    */
  // Futures
  /**
    * Futureは、非同期であることを気にせずに計算をシーケンスするモナドです。
    */
  import scala.concurrent.Future
  import scala.concurrent.ExecutionContext.Implicits.global

  def doSomethingLongRunning: Future[Int] = ???

  def doSomethingElseLongRunning: Future[Int] = ???

  //  def doSomethingVeryLongRunning: Future[Int] =
  //    for {
  //      result1 <- doSomethingLongRunning
  //      result2 <- doSomethingElseLongRunning
  //    } yield result1 + result2

  /**
    * ここでも、各ステップで実行するコードを指定し、flatMap がスレッドプールやスケジューラの複雑な処理を行います。
    * Future を十分に活用している方は、上のコードが各処理を順番に実行していることをご存知でしょう。
    * このことは、理解のために for を展開して flatMap へのネストされた呼び出しを表示すると、より明確になります。
    */
  def doSomethingVeryLongRunning: Future[Int] =
    doSomethingLongRunning.flatMap { result1 =>
      doSomethingElseLongRunning.map { result2 =>
        result1 + result2
      }
    }

  /**
    * シーケンス内の各フューチャーは、前のフューチャーから結果を受け取る関数によって生成されます。
    * 言い換えれば、計算の各ステップは、前のステップが終了してからしか開始できないということです。
    * このことは、図 9 の flatMap の型図から明らかになります。
    */
  // == 図9 ==

  /**
    * もちろん、先物を並列に走らせることもできますが、それはまた別の話で、また別の機会にお話ししましょう。
    * モナドはシーケンスが全てです。
    */
  // 4.1.1 Definition of a Monad

  /**
    * 上では flatMap の話をしただけですが、モナディックな振る舞いは正式には 2 つの操作で捕捉されます。
    * - pure, of type A => F[A];
    * - flatMap, of type (F[A], A => F[B]) => F[B].
    *
    * flatMap は、コンテクストから値を抽出して次のコンテクストを生成するという、すでに説明したシーケンスステップを提供します。
    * ここでは、Cats のモナド型クラスの簡略化版を紹介します。
    */
  //  trait Monad[F[_]] {
  //    def pure[A](value: A): F[A]
  //
  //    def flatMap[A, B](value: F[A])(func: A => F[B]): F[B]
  //  }

  /**
    * モナドの法則pure と flatMap は、意図しないグリッチや副作用のない自由な操作を可能にする一連の法則に従わなければなりません。
    * 左の同一性: pure を呼び出して func で結果を変換することは、func を呼び出すのと同じです。
    *
    * pure(a).flatMap(func) == func(a)
    *
    * 正しいアイデンティティ: 純粋なものを flatMap に渡すことは、何もしないことと同じです。
    * m.flatMap(pure) == m
    *
    * 連想性：2つの関数fとgに対するflatMappingは、fに対するflatMappingとgに対するflatMappingと同じです。
    * m.flatMap(f).flatMap(g) == m.flatMap(x => f(x).flatMap(g))
    */
  // 4.1.2 Exercise: Getting Func-y
  /**
    * すべてのモナドはファンクタでもあります。
    * 既存のメソッドである flatMap と pureMap を使えば、すべてのモナドに対して同じように map を定義することができます。
    */
  //  trait Monad[F[_]] {
  //    def pure[A](a: A): F[A]
  //
  //    def flatMap[A, B](value: F[A])(func: A => F[B]): F[B]
  //
  //    def map[A, B](value: F[A])(func: A => B): F[B] =
  //      ???
  //  }

  /**
    * 一見トリッキーに見えますが、型をたどっていくと、解決策は一つしかないことがわかります。
    * F[A] 型の値が渡されます。
    * 利用可能なツールを考えると、私たちにできることは一つしかありません。
    */
  /**
    * 2 番目のパラメータとして A => F[B] 型の関数が必要です。2 つの関数の構成要素があります。
    * それは、A => B 型の func パラメータと、A => F[A] 型の純粋な関数です。
    * これらを組み合わせると、次のような結果が得られます。
    */
  trait Monad[F[_]] {
    def pure[A](a: A): F[A]

    def flatMap[A, B](value: F[A])(func: A => F[B]): F[B]

    def map[A, B](value: F[A])(func: A => B): F[B] =
      flatMap(value)(v => pure(func(v)))
  }

  // 4.2 Monads in Cats
  /**
    * モナドをCatsの標準的な扱いにしてみましょう。
    * いつものように、型クラス、インスタンス、構文を見ていきましょう。
    */
  // 4.2.1 The Monad Type Class
  /**
    * モナド型クラスは cats.Monad です。
    * Monadは他の2つの型クラスを拡張しています。
    * FlatMap は flatMap メソッドを提供し、Applicative は pure を提供します。
    * ApplicativeはFunctorを継承しており、上の演習で見たようにすべてのモナドにマップメソッドを与えます。
    * アプリカティブについては第6章で説明します。
    *
    * ここでは、pureMap と flatMap を使用した例と、直接マッピングした例を示します。
    */
  //  import cats.Monad
  //  import cats.instances.option._ // for Monad
  //  import cats.instances.list._ // for Monad

  //  val opt1 = Monad[Option].pure(3)
  //  // opt1: Option[Int] = Some(3)
  //  val opt2 = Monad[Option].flatMap(opt1)(a => Some(a + 2))
  //  // opt2: Option[Int] = Some(5)
  //  val opt3 = Monad[Option].map(opt2)(a => 100 * a)
  //  // opt3: Option[Int] = Some(500)
  //
  //  val list1 = Monad[List].pure(3)
  //  // list1: List[Int] = List(3)
  //  val list2 = Monad[List].flatMap(List(1, 2, 3))(a => List(a, a * 10))
  //  // list2: List[Int] = List(1, 10, 2, 20, 3, 30)
  //  val list3 = Monad[List].map(list2)(a => a + 123)
  // list3: List[Int] = List(124, 133, 125, 143, 126, 153)

  /**
    * Monad は Functor からのメソッドをすべて含めて、他にも多くのメソッドを提供しています。
    * 詳細は scaladoc を参照してください。
    */
  // 4.2.2 Default Instances
  /**
    * Cats は、標準ライブラリのすべてのモナド（Option、List、Vector など）のインスタンスを cats.instance で提供しています。
    */
  import cats.instances.option._ // for Monad

  //  Monad[Option].flatMap(Option(1))(a => Option(a * 2))
  //  // res0: Option[Int] = Some(2)
  //
  //  import cats.instances.list._ // for Monad
  //
  //  Monad[List].flatMap(List(1, 2, 3))(a => List(a, a * 10))
  //  // res1: List[Int] = List(1, 10, 2, 20, 3, 30)
  //
  //  import cats.instances.vector._ // for Monad
  //
  //  Monad[Vector].flatMap(Vector(1, 2, 3))(a => Vector(a, a * 10))
  // res2: Vector[Int] = Vector(1, 10, 2, 20, 3, 30)

  /**
    * Cats は Future 用のモナドも提供しています。
    * Future クラス自体のメソッドとは異なり、モナドの pure および flatMap メソッドは暗黙の ExecutionContext パラメータを受け取ることができません (パラメータは Monad trait の定義に含まれていないため)。
    * これを回避するために、Cats では、Future クラスのモナドを召喚する際に、スコープ内に ExecutionContext を設定する必要があります。
    */
  //  import cats.instances.future._ // for Monad
  //  import scala.concurrent._
  //  import scala.concurrent.duration._
  //
  //  val fm = Monad[Future]
  // error: Could not find an instance of Monad for scala.concurrent.Future
  // val fm = Monad[Future]
  //          ^^^^^^^^^^^^^

  /**
    * ExecutionContext をスコープに入れることで、インスタンスを呼び出すために必要な暗黙の解決策が修正されました。
    */
  //  import scala.concurrent.ExecutionContext.Implicits.global

  //  val fm = Monad[Future]
  // fm: Monad[Future] = cats.instances.FutureInstances$$anon$1@5493a1be

  /**
    * Monad インスタンスは、キャプチャした ExecutionContext を使用して、その後の pure および flatMap への呼び出しに使用します。
    */
  //  val future = fm.flatMap(fm.pure(1))(x => fm.pure(x + 2))
  //
  //  Await.result(future, 1.second)
  // res4: Int = 3
  /**
    * 上記に加えて、Cats は標準ライブラリにはない新しいモナドを多数提供しています。
    * ここでは、これらのモナドをいくつか紹介します。
    */
  // 4.2.3 Monad Syntax

  /**
    * モナドの構文は3つのライブラリから来ています。
    * - cats.syntax.flatMap は flatMap の構文を提供します。
    * - cats.syntax.functor は map の構文を提供します。
    * - cats.syntax.applicative は pure の構文を提供します。
    */
  /**
    * 実際には、cats.implicitsから一度にすべてをインポートする方が簡単なことが多いです。
    * しかし、ここではわかりやすくするために個別のインポートを使用します。
    *
    * モナドのインスタンスを構築するには pure を使うことができます。
    * 欲しいインスタンスを曖昧にしないために、type パラメータを指定する必要があることがよくあります。
    */
  import cats.instances.option._ // for Monad
  import cats.instances.list._ // for Monad
  import cats.syntax.applicative._ // for pure

  1.pure[Option]
  // res5: Option[Int] = Some(1)
  1.pure[List]
  // res6: List[Int] = List(1)

  /**
    * OptionやListのようなScalaのモナド上でflatMapやmapメソッドを直接デモするのは難しいです。
    * その代わりに、ユーザが選択したモナドにラップされたパラメータに対して計算を行う汎用関数を書いてみましょう。
    */
  //  import cats.Monad
  //  import cats.syntax.functor._ // for map
  //  import cats.syntax.flatMap._ // for flatMap

  //  def sumSquare[F[_]: Monad](a: F[Int], b: F[Int]): F[Int] =
  //    a.flatMap(x => b.map(y => x*x + y*y))
  //
  //  import cats.instances.option._ // for Monad
  //  import cats.instances.list._   // for Monad
  //
  //  sumSquare(Option(3), Option(4))
  //  // res7: Option[Int] = Some(25)
  //  sumSquare(List(1, 2, 3), List(4, 5))
  //  // res8: List[Int] = List(17, 26, 20, 29, 25, 34)

  /**
    * このコードを for 内包を使って書き換えることができます。
    * コンパイラは、内包を flatMap と map で書き換え、正しい暗黙の変換を挿入して Monad
    */
  //  def sumSquare[F[_]: Monad](a: F[Int], b: F[Int]): F[Int] =
  //    for {
  //      x <- a
  //      y <- b
  //    } yield x*x + y*y
  //
  //  sumSquare(Option(3), Option(4))
  //  // res10: Option[Int] = Some(25)
  //  sumSquare(List(1, 2, 3), List(4, 5))
  //  // res11: List[Int] = List(17, 26, 20, 29, 25, 34)

  /**
    * 以上で、Catsのモナドの一般性について知る必要があることは、ほぼすべてわかったと思います。
    * では、Scala標準ライブラリにはない便利なモナドインスタンスを見てみましょう。
    */
  // 4.3 The Identity Monad

  /**
    * 前のセクションでは、異なるモナドを抽象化するメソッドを書くことで、Catsのフラットマップとマップの構文を実演しました。
    */
  //  import cats.Monad
  //  import cats.syntax.functor._ // for map
  //  import cats.syntax.flatMap._ // for flatMap

  //  def sumSquare[F[_]: Monad](a: F[Int], b: F[Int]): F[Int] =
  //    for {
  //      x <- a
  //      y <- b
  //    } yield x*x + y*y

  /**
    * このメソッドはオプションやリストではうまく動作しますが、プレーンな値を渡して呼び出すことはできません。
    */
  //   sumSquare(3, 4)
  // error: no type parameters for method sumSquare: (a: F[Int], b: F[Int])(implicit evidence$1: cats.Monad[F])F[Int] exist so that it can be applied to arguments (Int, Int)
  //  --- because ---
  // argument expression's type is not compatible with formal parameter type;
  //  found   : 3
  //  required: ?F[Int]
  // error: type mismatch;
  //  found   : Int(3)
  //  required: F[Int]
  // error: type mismatch;
  //  found   : Int(4)
  //  required: F[Int]

  /**
    * モナドに入っているか、モナドに全く入っていないパラメータで sumSquare を使うことができれば、信じられないほど便利です。
    * これにより、モナドと非モナドのコードを抽象化することができるようになります。
    * 幸いなことに、Cats はこのギャップを埋める Id 型を提供しています。
    */
  //  import cats.Id
  //
  //  sumSquare(3 : Id[Int], 4 : Id[Int])
  //  // res1: Id[Int] = 25

  /**
    * Id は、プレーンな値を使ってモナディックメソッドを呼び出すことを可能にします。
    * しかし、正確なセマンティクスを理解するのは困難です。sumSquare にパラメータを Id[Int] としてキャストすると、結果として Id[Int] が返ってきました。
    * どうなっているのでしょうか？ここではIdの定義を説明します。
    */
  type Id[A] = A

  /**
    * Id は、実際には原子型を単一パラメータの型コンストラクタに変換する型のエイリアスです。
    * 任意の型の任意の値を対応する Id
    */
  //  "Dave" : Id[String]
  //  // res2: Id[String] = "Dave"
  //  123 : Id[Int]
  //  // res3: Id[Int] = 123
  //  List(1, 2, 3) : Id[List[Int]]
  //  // res4: Id[List[Int]] = List(1, 2, 3)

  /**
    * Catsでは、FunctorやMonadをはじめとする様々な型クラスのインスタンスをIdに提供しています。
    * これらのクラスでは、map、flatMap、そしてプレーンな値を純粋に渡すことができます。
    */
  //  val a = Monad[Id].pure(3)
  //  // a: Id[Int] = 3
  //  val b = Monad[Id].flatMap(a)(_ + 1)
  // b: Id[Int] = 4

  //  import cats.syntax.functor._ // for map
  //  import cats.syntax.flatMap._ // for flatMap

  //  for {
  //    x <- a
  //    y <- b
  //  } yield x + y
  // res5: Id[Int] = 7

  /**
    * モノディックコードと非モノディックコードを抽象化する機能は非常に強力です。
    * 例えば、本番ではFutureを使って非同期的にコードを実行し、テストではIdを使って同期的にコードを実行することができます。
    * これを第8章の最初のケーススタディで見てみましょう。
    */
  // 4.3.1 Exercise: Monadic Secret Identities

  //  import cats.Id

  //
  //  def pure[A](v: A): Id[A] = v
  //
  //  def map[A, B](v: Id[A])(func: A => B): Id[B] = func(v)
  //
  //  def flatMap[A, B](v: Id[A])(func: A => Id[B]): Id[B] = func(v)

  // 4.4 Either
  /**
    * もう一つの便利なモナド、Scala標準ライブラリのEither型を見てみましょう。
    * cala 2.11以前のバージョンでは、EitherはmapやflatMapメソッドを持たないため、多くの人がモナドとは考えていませんでした。
    * しかし、Scala 2.12では、Eitherは右に偏るようになりました。
    */
  // 4.4.1 Left and Right Bias
  /**
    * Scala 2.11では、EitherにはデフォルトのmapやflatMapメソッドがありませんでした。
    * このため、Scala 2.11版のEitherは内包物に使うには不便でした。
    * ジェネレータ節の中に .right の呼び出しを挿入しなければなりませんでした。
    */
  val either1: Either[String, Int] = Right(10)
  val either2: Either[String, Int] = Right(32)

  for {
    a <- either1
    b <- either2
  } yield a + b

  /**
    * Scala 2.12では、Eitherが再設計されました。
    * 最近のEitherでは、右辺が成功例を表すと判断し、mapとflatMapを直接サポートしています。
    * これにより、理解がより快適になりました。
    */
  for {
    a <- either1
    b <- either2
  } yield a + b
  // res1: Either[String, Int] = Right(42)

  /**
    * Catsはこの動作をcats.syntax.anywhereインポートを介してScala 2.11にバックポートし、
    * サポートされているすべてのバージョンのScalaで右バイアスのEitherを使用できるようにしています。
    * Scala 2.12+では、このインポートを省略するか、何もせずにそのままにしておくことができます。
    */
  for {
    a <- either1
    b <- either2
  } yield a + b

  /**
    * 4.4.2 Creating Instances
    *
    * Left と Right のインスタンスを直接作成することに加えて、
    * asLeft と asRight の拡張メソッドを cats.syntax.anywhere からインポートすることもできます。
    */
  import cats.syntax.either._ // for asRight

  //  val a = 3.asRight[String]
  //  // a: Either[String, Int] = Right(3)
  //  val b = 4.asRight[String]
  // b: Either[String, Int] = Right(4)

  //  for {
  //    x <- a
  //    y <- b
  //  } yield x * x + y * y
  // res3: Either[String, Int] = Right(25)

  /**
    * これらの「スマートなコンストラクタ」は Left.apply や Right.apply に比べて、
    * Left や Right の代わりに Either 型の結果を返すという利点があります。
    * これは、以下の例のような、絞り込みすぎによる型推論の問題を回避するのに役立ちます。
    */
  //  def countPositive(nums: List[Int]): Right[Nothing, Int] =
  //    nums.foldLeft(Right(0)) { (accumulator, num) =>
  //      if (num > 0) {
  //        accumulator.map(_ + 1)
  //      } else {
  //        Left("Negative. Stopping!")
  //      }
  //    }

  // error: type mismatch;
  //  found   : scala.util.Either[Nothing,Int]
  //  required: scala.util.Right[Nothing,Int]
  //       accumulator.map(_ + 1)
  //       ^^^^^^^^^^^^^^^^^^^^^^
  // error: type mismatch;
  //  found   : scala.util.Left[String,Nothing]
  //  required: scala.util.Right[Nothing,Int]
  //       Left("Negative. Stopping!")
  //       ^^^^^^^^^^^^^^^^^^^^^^^^^^^

  /**
    * このコードのコンパイルに失敗するのには、次の2つの理由があります。
    *
    * 1. コンパイラはアキュムレータの型を Either ではなく Right と推定します。
    * 2. Right.apply の型パラメータを指定していないので、コンパイラは左のパラメータを Nothing として推論します。
    *
    * asRightに切り替えることで、これらの問題を回避することができます。
    * asRightの戻り値の型はEitherで、1つの型パラメータだけで完全に型を指定することができます。
    *
    * ※ asRightにすることで型を合わせることができるということっぽい。
    */
  def countPositive(nums: List[Int]): Either[String, Int] =
    nums.foldLeft(0.asRight[String]) { (accumulator, num) =>
      if (num > 0) {
        accumulator.map(_ + 1)
      } else {
        Left("Negative. Stopping!")
      }
    }

  countPositive(List(1, 2, 3))
  // res5: Either[String, Int] = Right(3)
  countPositive(List(1, -2, 3))
  // res6: Either[String, Int] = Left("Negative. Stopping!")

  /**
    * cats.syntax. either は Either コンパニオンオブジェクトにいくつかの便利な拡張メソッドを追加しました。
    * catchOnly と catchNonFatal メソッドはEitherインスタンスの例外処理を取得するのに最適です。
    */
  Either.catchOnly[NumberFormatException]("foo".toInt)
  // res7: Either[NumberFormatException, Int] = Left(
  //   java.lang.NumberFormatException: For input string: "foo"
  // )
  Either.catchNonFatal(sys.error("Badness"))
  // res8: Either[Throwable, Nothing] = Left(java.lang.RuntimeException: Badness)

  /**
    * 他のデータ型からEitherを作成する方法もあります。
    */
  Either.fromTry(scala.util.Try("foo".toInt))
  // res9: Either[Throwable, Int] = Left(
  //   java.lang.NumberFormatException: For input string: "foo"
  // )
  Either.fromOption[String, Int](None, "Badness")
  // res10: Either[String, Int] = Left("Badness")

  // 4.4.3 Transforming Eithers

  /**
    * cats.syntax.eitherも、Eitherインスタンスのためにいくつかの役立つ機能を拡張しました。
    *
    * Scala 2.11や2.12のユーザは、orElseやgetOrElseを使って右側から値を抽出したり、
    * デフォルトの値を返したりすることができました。
    */
  import cats.syntax.either._

  "Error".asLeft[Int].getOrElse(0)
  // res11: Int = 0
  "Error".asLeft[Int].orElse(2.asRight[String])
  // res12: Either[String, Int] = Right(2)

  /**
    * ensureメソッドは右側の値が述語の要件を満たすかどうかを確認することができます。
    */
  //  -1.asRight[String].ensure("Must be non-negative!")(_ > 0)
  // res13: Either[String, Int] = Left("Must be non-negative!")

  /**
    * recoverメソッドとrecoverWithメソッドは、Future上の名前の由来と同様のエラー処理を提供します。
    */
  "error".asLeft[Int].recover {
    case _: String => -1
  }
  // res14: Either[String, Int] = Right(-1)

  "error".asLeft[Int].recoverWith {
    case _: String => Right(-1)
  }
  // res15: Either[String, Int] = Right(-1)
  /**
    * map を補完するために leftMap と bimap メソッドがあります。
    */
  "foo".asLeft[Int].leftMap(_.reverse)
  // res16: Either[String, Int] = Left("oof")
  6.asRight[String].bimap(_.reverse, _ * 7)
  // res17: Either[String, Int] = Right(42)
  "bar".asLeft[Int].bimap(_.reverse, _ * 7)
  // res18: Either[String, Int] = Left("rab")

  /**
    * swapメソッドはleftとrightの型を入れかえることができます
    */
  123.asRight[String]
  // res19: Either[String, Int] = Right(123)
  123.asRight[String].swap
  // res20: Either[Int, String] = Left(123)

  /**
    * 最後に、Catsは、toOption、toList、toTry、toValidatedなどの変換メソッドのホストを追加しました。
    */
  /**
    * 4.4.4 Error Handling
    *
    * 　Eitherは通常、フェイルファストエラー処理を実装するために使用されます。
    * 通常どおり、flatMapを使用して計算をシーケンスします。
    * 1つの計算が失敗した場合、残りの計算は実行されません
    */
  for {
    a <- 1.asRight[String]
    b <- 0.asRight[String]
    c <- if (b == 0) "DIV0".asLeft[Int]
    else (a / b).asRight[String]
  } yield c * 100
  // res21: Either[String, Int] = Left("DIV0")

  /**
    * エラー処理にEitherを使用する場合、エラーを表すために使用するタイプを決定する必要があります。
    * これにはThrowableを使用できます
    */
  type Result[A] = Either[Throwable, A]

  /**
    * これにより、scala.util.Tryと同様のセマンティクスが得られます。
    * ただし、問題は、Throwableが非常に幅広いタイプであるということです。
    * どのタイプのエラーが発生したかについては（ほとんど）わかりません。
    * 別のアプローチは、プログラムで発生する可能性のあるエラーを表す代数的データ型を定義することです。
    */
  sealed trait LoginError extends Product with Serializable

  final case class UserNotFound(username: String) extends LoginError

  final case class PasswordIncorrect(username: String) extends LoginError

  final case class InvalidCharacter(username: String) extends LoginError

  case object UnexpectedError extends LoginError

  case class User(username: String, password: String)

  type LoginResult = Either[LoginError, User]

  /**
    * このアプローチは、Throwableで見た問題を解決します。
    * これにより、予想されるエラータイプの固定セットと、予想外のその他のすべてのキャッチオールが提供されます。
    * また、パターンマッチングを行う際の徹底的なチェックの安全性も得られます。
    */
  // Choose error-handling behaviour based on type:
  //  def handleError(error: LoginError): Unit =
  //    error match {
  //      case UserNotFound(u) =>
  ////        println(s"User not found: $u")
  //
  //      case PasswordIncorrect(u) =>
  ////        println(s"Password incorrect: $u")
  //
  //      // こんなやつの話？？
  //      case InvalidCharacter(u) =>
  ////        println(s"Invalid character: $u")
  //
  //      case UnexpectedError =>
  ////        println(s"Unexpected error")
  //    }

  //  val result1: LoginResult = User("dave", "passw0rd").asRight
  //  // result1: LoginResult = Right(User("dave", "passw0rd"))
  //  val result2: LoginResult = UserNotFound("dave").asLeft
  //  // result2: LoginResult = Left(UserNotFound("dave"))
  //  val result3: LoginResult = InvalidCharacter("D@ve").asLeft
  //
  //  result1.fold(handleError, println)
  //  // User(dave,passw0rd)
  //  result2.fold(handleError, println)

  // User not found: dave

  /**
    * Cats は、エラー処理に使用される Either ライクなデータ型を抽象化した MonadError と呼ばれる追加の型クラスを提供しています。
    * MonadError は、エラーの発生と処理のための追加操作を提供します。
    *
    * ここでは、MonadErrorの定義を簡略化しています。
    */
  trait MonadError[F[_], E] extends Monad[F] {
    // Lift an error into the `F` context:
    def raiseError[A](e: E): F[A]

    // Handle an error, potentially recovering from it:
    def handleErrorWith[A](fa: F[A])(f: E => F[A]): F[A]

    // Handle all errors, recovering from them:
    def handleError[A](fa: F[A])(f: E => A): F[A]

    // Test an instance of `F`,
    // failing if the predicate is not satisfied:
    def ensure[A](fa: F[A])(e: E)(f: A => Boolean): F[A]
  }

  /**
    * MonadErrorは2つの型のパラメータで定義されています。
    *
    * Fはmonadのタイプです
    * ＥはＦ内に含まれるエラーの種類である。
    *
    * これらのパラメータがどのように適合するかを示すために、Either の型クラスをインスタンス化した例を示します。
    */
  //  import cats.MonadError

  type ErrorOr[A] = Either[String, A]

  //  val monadError = MonadError[ErrorOr, String]

  /**
    * 応用エラー
    * 実際には、MonadErrorはApplicativeErrorという別の型クラスを拡張しています。
    * しかし、第6章まではApplicativeに遭遇することはありません。
    * セマンティクスはそれぞれの型クラスで同じなので、今のところこの詳細は無視できます。
    */
  /**
    * 4.5.2 Raising and Handling Errors
    *
    * MonadErrorの最も重要な2つのメソッドは、raiseErrorとhandleErrorWithです。
    * raiseErrorは、失敗を表すインスタンスを作成することを除いて、Monadの純粋なメソッドのようなものです。
    */
  //  val success = monadError.pure(42)
  //  // success: ErrorOr[Int] = Right(42)
  //  val failure = monadError.raiseError("Badness")
  // failure: ErrorOr[Nothing] = Left("Badness")

  /**
    * handleErrorWith は raiseError を補完するものです。
    * これにより、Futureのrecoverメソッドと同様に、エラーを消費して(可能性としては)成功に変えることができます。
    */
  //  monadError.handleErrorWith(failure) {
  //    case "Badness" =>
  //      monadError.pure("It's ok")
  //
  //    case _ =>
  //      monadError.raiseError("It's not ok")
  //  }
  // res0: ErrorOr[String] = Right("It's ok")

  /**
    * すべてのエラーが起こりうる処理がわかっているのであれば、 handleWith を使うことができます。
    */
  //  monadError.handleError(failure) {
  //    case "Badness" => 42
  //
  //    case _ => -1
  //  }
  // res1: ErrorOr[Int] = Right(42)

  /**
    * フィルタのような振る舞いを実装した ensure という便利なメソッドがもう一つあります。
    * 成功したモナドの値を述語でテストし、述語がfalseを返した場合に発生するエラーを指定します。
    */
  //  monadError.ensure(success)("Number too low!")(_ > 1000)
  // res2: ErrorOr[Int] = Left("Number too low!")

  /**
    * Catsは、cats.syntax.applicativeErrorを介してraiseErrorとhandleErrorWithの構文を提供し、cats.syntax.monadErrorを介してensureを提供しています。
    */
  import cats.syntax.applicative._ // for pure
  import cats.syntax.applicativeError._ // for raiseError etc

  //  val success = 42.pure[ErrorOr]
  //  // success: ErrorOr[Int] = Right(42)
  //  val failure = "Badness".raiseError[ErrorOr, Int]
  //  // failure: ErrorOr[Int] = Left("Badness")
  //  failure.handleErrorWith {
  //    case "Badness" =>
  //      256.pure

  //    case _ =>
  //      ("It's not ok").raiseError
  //  }
  // res4: ErrorOr[Int] = Right(256)
  //  success.ensure("Number to low!")(_ > 1000)
  // res5: ErrorOr[Int] = Left("Number to low!")

  /**
    * これらのメソッドには他にも便利な種類があります。
    * 詳細は cats.MonadError と cats.ApplicativeError のソースを参照してください。
    */
  /**
    * 4.5.3 Instances of MonadError
    *
    * Catsは、Either、Future、Tryを含む多数のデータ型に対してMonadErrorのインスタンスを提供しています。
    * Either のインスタンスは任意のエラータイプにカスタマイズ可能ですが、Future と Try のインスタンスは常に Throwables としてエラーを表現します。
    */
  import scala.util.Try
  import cats.instances.try_._ // for MonadError

  val exn: Throwable =
    new RuntimeException("It's all gone wrong")

  exn.raiseError[Try, Int]
  // res6: Try[Int] = Failure(java.lang.RuntimeException: It's all gone wrong)

  /**
    * 4.5.4 Exercise: Abstracting
    *
    * 以下のシグネチャを持つ validateAdult メソッドを実装します。
    */
  //  def validateAdult[F[_]](age: Int)(
  //      implicit me: MonadError[F, Throwable]): F[Int] =
  //    ???

  /**
    * 18歳以上の年齢を渡された場合は、その値を成功として返すべきです。
    * それ以外の場合は、IllegalArgumentExceptionとしてエラーを返します。
    * 以下に使用例を示します。
    */
  //  validateAdult[Try](18)
  //  // res7: Try[Int] = Success(18)
  //  validateAdult[Try](8)
  // res8: Try[Int] = Failure(
  //   java.lang.IllegalArgumentException: Age must be greater than or equal to 18
  // )
  type ExceptionOr[A] = Either[Throwable, A]
  //  validateAdult[ExceptionOr](-1)
  //  // res9: ExceptionOr[Int] = Left(
  //  //   java.lang.IllegalArgumentException: Age must be greater than or equal to 18
  //  // )

  /**
    * これを解決するには pure と raiseError を使用します。
    * これらのメソッドでは、型の推論を助けるために型パラメータを使用していることに注意してください。
    */
  //  def validateAdult[F[_]](age: Int)(
  //      implicit me: MonadError[F, Throwable]): F[Int] =
  //    if (age >= 18) age.pure[F]
  //    else
  //      new IllegalArgumentException("Age must be greater than or equal to 18")
  //        .raiseError[F, Int]

  /**
    * 4.6.1 Eager, Lazy, Memoized, Oh My!
    *
    * 評価モデルのこれらの用語は何を意味しているのでしょうか？いくつかの例を見てみましょう。
    * まず、Scala の vals を見てみましょう。目に見える副作用のある計算を使って評価モデルを見ることができます。
    * 次の例では、xの値を計算するコードは、アクセス時ではなく、xが定義されている場所で発生します。
    * x にアクセスすると、コードを再実行することなく、格納されている値が呼び出されます。
    */
  //  val x = {
  //    println("Computing X")
  //    math.random
  //  }
  // Computing X
  // x: Double = 0.15241729989551633

  //  x // first access
  // res0: Double = 0.15241729989551633 // first access
  //  x // second access
  // res1: Double = 0.15241729989551633

  /**
    * これは、呼び出し単位の評価の例です。計算が定義されている点で評価されます(eager)。
    * 計算は一度だけ評価されます。(メモされる)
    * defを使った例を見てみましょう。
    * 以下のyを計算するコードは使うまで実行されず、アクセスするたびに再実行されます。
    */
  //  def y = {
  //    println("Computing Y")
  //    math.random
  //  }

  //  y // first access
  // Computing Y
  // res2: Double = 0.6963618800921411 // first access
  //  y // second access
  // Computing Y
  // res3: Double = 0.7321640587866993

  /**
    * これらは、コールバイネーム評価のプロパティです。
    * 計算は使用時に評価されます(遅延)。
    * 計算は使用されるたびに評価されます (メモされません)。
    * 最後になりましたが、レイジー・バルは、コール・バイ・ニーズ評価の例です。
    * 以下のzを計算するコードは、初めて使用するまで実行されません（レイジー）。
    * その結果はキャッシュされ、その後のアクセスで再利用されます（memoized）。
    */
  //  lazy val z = {
  //    println("Computing Z")
  //    math.random
  //  }
  //
  //  z // first access
  //  // Computing Z
  //  // res4: Double = 0.18457255119783122 // first access
  //  z // second access
  //  // res5: Double = 0.18457255119783122

  /**
    * まとめてみましょう。注目すべき性質は2つあります。
    * 定義の時点での評価（eager）と使用の時点での評価（lazy）。
    * 値は一度評価されると保存される(メモされる)か、されない(メモされない)かのどちらかになります。
    * これらのプロパティには3つの組み合わせが考えられます。
    *
    * call-by-valueはeagerでメモされています。
    * call-by-nameは遅延していてメモされていない。
    * lazyでメモされているcall-by-need。
    * 最終的にはeagerでメモされていない組み合わせは無理です。
    */
  /**
    * 4.6.2 Eval’s Models of Evaluation
    *
    * Evalには3つのサブタイプがあります。Now、Always、Laterです。
    * これらはそれぞれ call-by-value, call-by-name, call-by-need に対応しています。
    * これらは、3つのクラスのインスタンスを作成して Eval として返すコンストラクタメソッドで構成されています。
    */
  import cats.Eval

  val now = Eval.now(math.random + 1000)
  // now: Eval[Double] = Now(1000.020590704322)
  val always = Eval.always(math.random + 3000)
  // always: Eval[Double] = cats.Always@4d8ca6eb
  val later = Eval.later(math.random + 2000)
  // later: Eval[Double] = cats.Later@601dc0b2

  /**
    * Evalの結果は、その値のメソッドを使って抽出することができます。
    */
  now.value
  // res6: Double = 1000.020590704322
  always.value
  // res7: Double = 3000.97102818157
  later.value
  // res8: Double = 2000.0126977436273

  /**
    * 各タイプの Eval は、上記で定義された評価モデルのいずれかを使用して結果を計算します。
    * Eval.nowは現在の値をキャプチャします。
    * そのセマンティクスは、val-eagerとmemoizedに似ています。
    */
  //  val x = Eval.now {
  //    println("Computing X")
  //    math.random
  //  }
  // Computing X
  // x: Eval[Double] = Now(0.681816469770503)

  //  x.value // first access
  //  // res10: Double = 0.681816469770503 // first access
  //  x.value // second access
  //  // res11: Double = 0.681816469770503

  /**
    * Eval.alwaysは常に遅延計算を捕捉します。
    */
  //  val y = Eval.always {
  //    println("Computing Y")
  //    math.random
  //  }
  // y: Eval[Double] = cats.Always@414a351

  //  y.value // first access
  //  // Computing Y
  //  // res12: Double = 0.09982997820703643 // first access
  //  y.value // second access
  //  // Computing Y
  //  // res13: Double = 0.34240334819463436

  /**
    * 最後に、 Eval.later は、遅延してメモされた計算を捕捉します。
    */
  //  val z = Eval.later {
  //    println("Computing Z")
  //    math.random
  //  }
  // z: Eval[Double] = cats.Later@b0a344a

  //  z.value // first access
  //  // Computing Z
  //  // res14: Double = 0.3604236919233441 // first access
  //  z.value // second access
  //  // res15: Double = 0.3604236919233441

  /**
    * |  Scala     |	Cats    |	Properties          |
    * |   val	    |  Now    |	eager, memoized     |
    * |   def	    | Always	|  lazy, not memoized |
    * |  lazy val  |	Later	  |  lazy, memoized     |
    */
  /**
    * 4.6.3 Eval as a Monad
    *
    * 他のモナドと同様に、 Eval の map と flatMap メソッドは、計算を連鎖に追加します。
    * しかし、この場合、連鎖は関数のリストとして明示的に格納されます。
    * これらの関数は、結果を要求するために Eval の value メソッドを呼び出すまで実行されません。
    */
  //  val greeting = Eval
  //    .always {
  //      println("Step 1"); "Hello"
  //    }
  //    .map { str =>
  //      println("Step 2"); s"$str world"
  //    }
  //  // greeting: Eval[String] = cats.Eval$$anon$4@2319703e
  //
  //  greeting.value
  // Step 1
  // Step 2
  // res16: String = "Hello world"

  /**
    * 元の Eval インスタンスのセマンティクスは維持されますが、
    * マッピング関数は常にオンデマンドで呼び出されます（def semantics）。
    */
  //  val ans = for {
  //    a <- Eval.now {
  //      println("Calculating A"); 40
  //    }
  //    b <- Eval.always {
  //      println("Calculating B"); 2
  //    }
  //  } yield {
  //    println("Adding A and B")
  //    a + b
  //  }
  // Calculating A
  // ans: Eval[Int] = cats.Eval$$anon$4@2d0f2cbf

  //  ans.value // first access
  //  // Calculating B
  //  // Adding A and B
  //  // res17: Int = 42 // first access
  //  ans.value // second access
  //  // Calculating B
  //  // Adding A and B
  //  // res18: Int = 42

  /**
    * Evalにはmemoizeメソッドがあり、計算の連鎖をmemoizeすることができます。
    * memoizeを呼び出すまでの連鎖の結果はキャッシュされますが、呼び出し後の計算は元のセマンティクスを保持します。
    */
  //  val saying = Eval
  //    .always {
  //      println("Step 1"); "The cat"
  //    }
  //    .map { str =>
  //      println("Step 2"); s"$str sat on"
  //    }
  //    .memoize
  //    .map { str =>
  //      println("Step 3"); s"$str the mat"
  //    }
  // saying: Eval[String] = cats.Eval$$anon$4@ca01c64

  //  saying.value // first access
  //  // Step 1
  //  // Step 2
  //  // Step 3
  //  // res19: String = "The cat sat on the mat" // first access
  //  saying.value // second access
  // Step 3
  // res20: String = "The cat sat on the mat"

  /**
    * 4.6.4 Trampolining and Eval.defer
    *
    * Eval の便利な特性の一つは、map と flatMap メソッドがトランポリン処理されていることです。
    * これは、スタックフレームを消費せずに map や flatMap の呼び出しを任意にネストすることができることを意味します。
    * この性質を「スタックの安全性」と呼んでいます。
    * 例えば、階乗を計算するための以下の関数を考えてみましょう。
    */
  //  def factorial(n: BigInt): BigInt =
  //    if (n == 1) n else n * factorial(n - 1)

  /**
    * このメソッドをスタックオーバーフローさせるのは比較的簡単です。
    */
  //  factorial(50000)
  // java.lang.StackOverflowError
  //   ...

  /**
    * Evalを使ってメソッドを書き換えればスタックセーフになります。
    */
  //  def factorial(n: BigInt): Eval[BigInt] =
  //    if(n == 1) {
  //      Eval.now(n)
  //    } else {
  //      factorial(n - 1).map(_ * n)
  //    }
  //
  //  factorial(50000).value
  //  // java.lang.StackOverflowError
  //  //   ...

  /**
    * おっと! スタックが爆発してしまいました。
    * これは、Evalのマップメソッドで作業を開始する前に、すべての再帰的な呼び出しを行っているためです。
    * Eval.deferは既存のEvalのインスタンスを受け取り、その評価を延期します。
    * deferメソッドはmapやflatMapのようにトランポリン処理されているので、既存の演算スタックを安全にするための手っ取り早い方法として使うことができます。
    */
  //  def factorial(n: BigInt): Eval[BigInt] =
  //    if (n == 1) {
  //      Eval.now(n)
  //    } else {
  //      Eval.defer(factorial(n - 1).map(_ * n))
  //    }
  //
  //  factorial(50000).value
  // res: A very big value

  /**
    * Evalは、非常に大きな計算やデータ構造を扱う際にスタックの安全性を強制するための便利なツールです。
    * しかし、トランポリンは自由ではないことを心に留めておかなければなりません。
    * ヒープ上に関数オブジェクトのチェーンを作成することでスタックを消費することを回避します。
    * 計算をネストできる深さにはまだ限界がありますが、スタックではなくヒープのサイズによって制限されます。
    */
  /**
    * 4.6.5 Exercise: Safer Folding using Eval
    *
    * 以下の foldRight のナイーブな実装はスタックセーフではありません。
    * Eval を使ってそうしてください。
    */
  //  def foldRight[A, B](as: List[A], acc: B)(fn: (A, B) => B): B =
  //    as match {
  //      case head :: tail =>
  //        fn(head, foldRight(tail, acc)(fn))
  //      case Nil =>
  //        acc
  //    }

  //  def foldRightEval[A, B](as: List[A], acc: Eval[B])(
  //      fn: (A, Eval[B]) => Eval[B]): Eval[B] =
  //    as match {
  //      case head :: tail =>
  //        Eval.defer(fn(head, foldRightEval(tail, acc)(fn)))
  //      case Nil =>
  //        acc
  //    }

  /**
    * 4.5 Aside: Error Handling and MonadError
    *
    * Cats は、エラー処理に使用される Either ライクなデータ型を抽象化したMonadError と呼ばれる追加の型クラスを提供しています。
    * MonadError は、エラーの発生と処理のための追加操作を提供します。
    *
    * 4.5.1 The MonadError Type Class
    * ここでは、MonadErrorの定義を簡略化しています。
    */
  //  trait MonadError[F[_], E] extends Monad[F] {
  //    // Lift an error into the `F` context:
  //    def raiseError[A](e: E): F[A]
  //
  //    // Handle an error, potentially recovering from it:
  //    // 1つのエラーを操作し、そのエラーをリカバリする
  //    def handleErrorWith[A](fa: F[A])(f: E => F[A]): F[A]
  //
  //    // Handle all errors, recovering from them:
  //    // すべてのエラーを操作し、それらをリカバリする
  //    def handleError[A](fa: F[A])(f: E => A): F[A]
  //
  //    // Test an instance of `F`,
  //    // failing if the predicate is not satisfied:
  //    def ensure[A](fa: F[A])(e: E)(f: A => Boolean): F[A]
  //  }

  /**
    * MonadErrorは2つの型パラメータで定義されています。
    * Fはモナドの型です。
    * E は F に含まれるエラーのタイプです。
    * sこれらのパラメータがどのように適合するかを示すために、Either の型クラスをインスタンス化した例を示します。
    */
  //  import cats.MonadError
  //  import cats.instances.either._ // for MonadError

  //  type ErrorOr[A] = Either[String, A]

  //  val monadError = MonadError[ErrorOr, String]

  /**
    * ApplicativeError
    * 実際には、MonadErrorはApplicativeErrorという別の型クラスを拡張しています。
    * しかし、第6章まではApplicativeに遭遇することはありません。
    * セマンティクスはそれぞれの型クラスで同じなので、今のところこの詳細は無視できます。
    *
    * 4.5.2 Raising and Handling Errors
    * MonadErrorの最も重要な2つのメソッドは、raiseErrorとhandleErrorWithです。
    * raiseErrorは、失敗を表すインスタンスを作成することを除いて、Monadの純粋なメソッドのようなものです。
    *
    */
  //  val success = monadError.pure(42)
  // success: ErrorOr[Int] = Right(42)
  //  val failure = monadError.raiseError("Badness")
  // failure: ErrorOr[Nothing] = Left("Badness")

  /**
    * handleErrorWith は raiseError を補完するものです。
    * これにより、Futureのrecoverメソッドと同様に、エラーを消費して(可能性としては)成功に変えることができます。
    */
  //  monadError.handleErrorWith(failure) {
  //    case "Badness" =>
  //      monadError.pure("It's ok")
  //
  //    case _ =>
  //      monadError.raiseError("It's not ok")
  //  }
  // res0: ErrorOr[String] = Right("It's ok")

  /**
    * すべての可能なエラーを処理できることがわかっているのであれば、 handleWith を使うことができます。
    */
  //  monadError.handleError(failure) {
  //    case "Badness" => 42
  //
  //    case _ => -1
  //  }
  //   res1: ErrorOr[Int] = Right(42)

  /**
    * フィルタのような振る舞いを実装した ensure という便利なメソッドがもう一つあります。
    * 成功したモナドの値を述語でテストし、述語がfalseを返した場合に発生するエラーを指定します。
    */
  //  monadError.ensure(success)("Number too low!")(_ > 1000)
  // res2: ErrorOr[Int] = Left("Number too low!")
  /**
    * Catsは、cats.syntax.applicativeErrorを介してraiseErrorとhandleErrorWithの構文を提供し、cats.syntax.monadErrorを介してensureを提供しています。
    */
  //  import cats.syntax.applicative._ // for pure
  //  import cats.syntax.applicativeError._ // for raiseError etc
  //  import cats.syntax.monadError._ // for ensure

  //  val success = 42.pure[ErrorOr]
  //  // success: ErrorOr[Int] = Right(42)
  //  val failure = "Badness".raiseError[ErrorOr, Int]
  //  // failure: ErrorOr[Int] = Left("Badness")
  //  failure.handleErrorWith {
  //    case "Badness" =>
  //      256.pure
  //
  //    case _ =>
  //      ("It's not ok").raiseError
  //  }
  //  // res4: ErrorOr[Int] = Right(256)
  //  success.ensure("Number to low!")(_ > 1000)
  // res5: ErrorOr[Int] = Left("Number to low!")

  /**
    * これらのメソッドには他にも便利な亜種があります。
    * 詳細は cats.MonadError と cats.ApplicativeError のソースを参照してください。
    *
    * 4.5.3 Instances of MonadError
    *
    * Catsは、Either、Future、Tryを含む多数のデータ型に対してMonadErrorのインスタンスを提供しています。
    * Either のインスタンスは任意のエラータイプにカスタマイズ可能ですが、Future と Try のインスタンスは常に Throwables としてエラーを表現します。
    */
  import scala.util.Try
  import cats.instances.try_._ // for MonadError
  //  import cats.syntax.applicative._
  import cats.syntax.applicativeError._
  //  import cats.instances.either._

  //  val exn: Throwable =
  //    new RuntimeException("It's all gone wrong")

  exn.raiseError[Try, Int]
  // res6: Try[Int] = Failure(java.lang.RuntimeException: It's all gone wrong)

  /**
    * 4.5.4 Exercise: Abstracting
    *
    * 以下のシグネチャを持つ validateAdult メソッドを実装します。
    */
  //  def validateAdult[F[_]](age: Int)(
  //      implicit me: MonadError[F, Throwable]): F[Int] = {
  //    val exn: Throwable =
  //      new IllegalArgumentException("Age must be greater than or equal to 18")
  //    if (age >= 18) age.pure[F]
  //    else exn.raiseError[F, Int]
  //    validateAdult[Try](18)
  //    validateAdult[Try](8)
  //    type ExceptionOr[A] = Either[Throwable, A]
  //    validateAdult[ExceptionOr](-1)
  //
  //  }
  //
  //  /**
  //    * 18歳以上の年齢を渡された場合は、その値を成功として返すべきです。
  //    * それ以外の場合は、IllegalArgumentExceptionとしてエラーを返します。
  //    * 以下に使用例を示します。
  //    */
  //  validateAdult[Try](18)
  //
  //  // res7: Try[Int] = Success(18)
  //  validateAdult[Try](8)
  //  // res8: Try[Int] = Failure(
  //  //   java.lang.IllegalArgumentException: Age must be greater than or equal to 18
  //  // )
  //  type ExceptionOr[A] = Either[Throwable, A]
  //  validateAdult[ExceptionOr](-1)
  //  // res9: ExceptionOr[Int] = Left(
  //  //   java.lang.IllegalArgumentException: Age must be greater than or equal to 18
  //  // )

  /**
    * 4.6 The Eval Monad
    * cats.Evalは評価の異なるモデルを抽象化するためのモナドです。
    * 一般的には、eagerとlazyの2つの評価モデルについて話しています。
    * Evalは結果をメモすることもでき、これによりcall-by-need評価が可能になります。
    * Evalはスタックセーフであり、スタックを爆発させることなく、非常に深い再帰処理でも使用できます。
    *
    * 4.6.1 Eager, Lazy, Memoized, Oh My!
    *
    * 評価モデルのこれらの用語は何を意味しているのでしょうか？いくつかの例を見てみましょう。
    *
    * まず、Scala の vals を見てみましょう。
    * 目に見える副作用のある計算を使って評価モデルを見ることができます。
    * 次の例では、xの値を計算するコードは、アクセス時ではなく、xが定義されている場所で発生します。
    * x にアクセスすると、コードを再実行することなく、格納されている値が呼び出されます。
    */
  //  val x = {
  //    println("Computing X")
  //    math.random
  //  }
  // Computing X
  // x: Double = 0.15241729989551633

  //  x // first access
  // res0: Double = 0.15241729989551633 // first access
  //  x // second access
  // res1: Double = 0.15241729989551633

  /**
    * これは、コールバイバリュー評価の例です。
    *
    * - 計算が定義されている点で評価されます(eager);
    * - 計算は一度だけ評価されます（メモされます）。
    *
    * defを使った例を見てみましょう。 以下のyを計算するコードは使うまで実行されず、アクセスするたびに再実行されます。
    */
  //  def y = {
  //    println("Computing Y")
  //    math.random
  //  }

  //  y // first access
  // Computing Y
  // res2: Double = 0.6963618800921411 // first access
  //  y // second access
  // Computing Y
  // res3: Double = 0.7321640587866993

  /**
    * これらは、コールバイネーム評価のプロパティです。
    * - 計算は使用時に評価されます(遅延)。
    * - 計算は使用されるたびに評価されます (メモされません)。
    * 最後になりましたが、レイジー・バルは、コール・バイ・ニーズ評価の例です。
    * 以下のzを計算するコードは、初めて使用するまで実行されません（レイジー）。
    * その結果はキャッシュされ、その後のアクセスで再利用されます（memoized）。
    */
  //  lazy val z = {
  //    println("Computing Z")
  //    math.random
  //  }

  //  z // first access
  // Computing Z
  // res4: Double = 0.18457255119783122 // first access
  //  z // second access
  // res5: Double = 0.18457255119783122

  /**
    * まとめてみましょう。気になる性質は2つあります。
    * - 定義の時点での評価（eager）対使用の時点での評価（lazy）
    * - 値は一度評価されたら保存される(メモされる)か、されない(メモされない)かのどちらかになります。
    * これらの特性の組み合わせは3つの可能性があります。
    * - eagerにメモしているコールバイバリュー。
    * - lazyでメモされていないコールバイネーム。
    * - lazyでメモしているコールバイニーズ
    *
    * 最後の組み合わせ、eagerでしていてメモしていない、というのはありえない。
    *
    * 4.6.2 Eval’s Models of Evaluation
    *
    * Evalには3つのサブタイプがあります。
    * Now、Always、Laterです。これらはそれぞれ call-by-value, call-by-name, call-by-need に対応しています。
    * これらは、3つのクラスのインスタンスを作成して Eval として返すコンストラクタメソッドで構成されています。
    */
  import cats.Eval

  //  val now = Eval.now(math.random + 1000)
  //  // now: Eval[Double] = Now(1000.020590704322)
  //  val always = Eval.always(math.random + 3000)
  //  // always: Eval[Double] = cats.Always@4d8ca6eb
  //  val later = Eval.later(math.random + 2000)
  //  // later: Eval[Double] = cats.Later@601dc0b2

  /**
    * Evalの結果は、その値のメソッドを使って抽出することができます。
    */
  now.value
  // res6: Double = 1000.020590704322
  always.value
  // res7: Double = 3000.97102818157
  later.value
  // res8: Double = 2000.0126977436273

  /**
    * 各タイプの Eval は、上記で定義された評価モデルのいずれかを使用して結果を計算します。
    * Eval.nowは現在の値をキャプチャします。
    * そのセマンティクスは、val-eagerとmemoizedに似ています。
    */
  //  val x = Eval.now {
  //    println("Computing X")
  //    math.random
  //  }
  //  // Computing X
  //  // x: Eval[Double] = Now(0.681816469770503)
  //
  //  x.value // first access
  //  // res10: Double = 0.681816469770503 // first access
  //  x.value // second access
  //  // res11: Double = 0.681816469770503
  /**
    * Eval.alwaysは常に遅延計算を捕捉します。
    */
  //  val y = Eval.always{
  //    println("Computing Y")
  //    math.random
  //  }
  //  // y: Eval[Double] = cats.Always@414a351
  //
  //  y.value // first access
  //  // Computing Y
  //  // res12: Double = 0.09982997820703643 // first access
  //  y.value // second access
  //  // Computing Y
  //  // res13: Double = 0.34240334819463436

  /**
    * 最後に、 Eval.later は、遅延してメモされた計算を捕捉します。
    */
  //  val z = Eval.later{
  //    println("Computing Z")
  //    math.random
  //  }
  //  // z: Eval[Double] = cats.Later@b0a344a
  //
  //  z.value // first access
  //  // Computing Z
  //  // res14: Double = 0.3604236919233441 // first access
  //  z.value // second access
  //  // res15: Double = 0.3604236919233441

  /**
    * 3つの行動をまとめると以下のようになります。
    *
    * |Scala	 |Cats	  |Properties|
    * |val	   |Now	    |eager, memoized|
    * |def	   |Always	|lazy, not memoized|
    * lazy val |Later   |	lazy, memoized|
    *
    * 4.6.3 Eval as a Monad
    *
    * 他のモナドと同様に、 Eval の map と flatMap メソッドは、計算を連鎖に追加します。
    * しかし、この場合、連鎖は関数のリストとして明示的に格納されます。
    * これらの関数は、結果を要求するために Eval の value メソッドを呼び出すまで実行されません。
    */
  //  val greeting = Eval
  //    .always {
  //      println("Step 1"); "Hello"
  //    }
  //    .map { str =>
  //      println("Step 2"); s"$str world"
  //    }
  // greeting: Eval[String] = cats.Eval$$anon$4@2319703e

  //  greeting.value
  // Step 1
  // Step 2
  // res16: String = "Hello world"

  /**
    * 元の Eval インスタンスのセマンティクスは維持されますが、マッピング関数は常にオンデマンドで呼び出されます（def semantics）。
    */
  //  val ans = for {
  //    a <- Eval.now {
  //      println("Calculating A"); 40
  //    }
  //    b <- Eval.always {
  //      println("Calculating B"); 2
  //    }
  //  } yield {
  //    println("Adding A and B")
  //    a + b
  //  }
  // Calculating A
  // ans: Eval[Int] = cats.Eval$$anon$4@2d0f2cbf

  //  ans.value // first access
  //  // Calculating B
  //  // Adding A and B
  //  // res17: Int = 42 // first access
  //  ans.value // second access
  //  // Calculating B
  //  // Adding A and B
  //  // res18: Int = 42

  /**
    * Evalにはmemoizeメソッドがあり、計算の連鎖をmemoizeすることができます。
    * memoizeを呼び出すまでの連鎖の結果はキャッシュされますが、呼び出し後の計算は元のセマンティクスを保持します。
    */
  //  val saying = Eval
  //    .always {
  //      println("Step 1"); "The cat"
  //    }
  //    .map { str =>
  //      println("Step 2"); s"$str sat on"
  //    }
  //    .memoize
  //    .map { str =>
  //      println("Step 3"); s"$str the mat"
  //    }
  //  // saying: Eval[String] = cats.Eval$$anon$4@ca01c64
  //
  //  saying.value // first access
  //  // Step 1
  //  // Step 2
  //  // Step 3
  //  // res19: String = "The cat sat on the mat" // first access
  //  saying.value // second access
  //  // Step 3
  //  // res20: String = "The cat sat on the mat"

  /**
    * 4.6.4 Trampolining and Eval.defer
    *
    * Eval の便利な特性の一つは、map と flatMap メソッドがトランポリン処理されていることです。
    * これは、スタックフレームを消費せずに map や flatMap の呼び出しを任意にネストすることができることを意味します。
    * この性質を「スタックの安全性」と呼んでいます。
    * 例えば、階乗を計算するための以下の関数を考えてみましょう。
    */
  def factorial(n: BigInt): BigInt =
    if (n == 1) n else n * factorial(n - 1)

  /**
    * このメソッドをスタックオーバーフローさせるのは比較的簡単です。
    */
//  factorial(50000)
  // java.lang.StackOverflowError
  //   ...

  /**
    * Evalを使ってメソッドを書き換えればスタックセーフになります。
    */
  //  def factorial(n: BigInt): Eval[BigInt] =
  //    if (n == 1) {
  //      Eval.now(n)
  //    } else {
  //      factorial(n - 1).map(_ * n)
  //    }
  //
  //  factorial(50000).value
  //  // java.lang.StackOverflowError
  //  //   ...

  /**
    * おっと! スタックが爆発してしまいました。
    * これは、Evalのマップメソッドで作業を開始する前に、すべての再帰的な呼び出しを行っているためです。
    * Eval.deferは既存のEvalのインスタンスを受け取り、その評価を延期します。
    * deferメソッドはmapやflatMapのようにトランポリン処理されているので、既存の演算スタックを安全にするための手っ取り早い方法として使うことができます。
    */
  //  def factorial(n: BigInt): Eval[BigInt] =
  //    if (n == 1) {
  //      Eval.now(n)
  //    } else {
  //      Eval.defer(factorial(n - 1).map(_ * n))
  //    }
  //
  //  factorial(50000).value
  //  // res: A very big value

  /**
    * Evalは、非常に大きな計算やデータ構造を扱う際にスタックの安全性を強制するための便利なツールです。
    * しかし、トランポリンは自由ではないことを心に留めておかなければなりません。
    * ヒープ上に関数オブジェクトのチェーンを作成することでスタックを消費することを回避します。
    * 計算をネストできる深さにはまだ限界がありますが、スタックではなくヒープのサイズによって制限されます。
    *
    * 4.6.5 Exercise: Safer Folding using Eval
    *
    * 以下の foldRight のナイーブな実装はスタックセーフではありません。Eval を使ってそうしてください。
    */
  def foldRightEval[A, B](as: List[A], acc: Eval[B])(
      fn: (A, Eval[B]) => Eval[B]): Eval[B] =
    as match {
      case head :: tail =>
        Eval.defer(fn(head, foldRight(tail, acc)(fn)))
      case Nil =>
        Eval.defer(acc)
    }

  def foldRight[A, B](as: List[A], acc: B)(fn: (A, B) => B): B =
    foldRightEval(as, Eval.now(acc)) { (a, b) =>
      b.map(fn(a, _))
    }.value

  foldRight((1 to 100000).toList, 0L)(_ + _)
  // res24: Long = 5000050000L
  /**
    * 4.7 The Writer Monad
    *
    * cats.data.Writerは、計算と一緒にログを運ぶためのモナドです。
    * これを使って、メッセージやエラー、計算に関する追加データを記録し、最終結果と一緒にログを抽出することができます。
    *
    * Writerの一般的な用途としては、マルチスレッド計算のステップのシーケンスを記録することがあります。
    * Writerでは、計算のログが結果に紐付けられているので、ログを混在させることなく同時に計算を実行することができます。
    *
    * Cats Data Types
    *
    * Writer は 私達が見てきたcats.data パッケージで初めてのデータ型です。
    * このパッケージは、有用なセマンティクスを生み出す様々な型クラスのインスタンスを提供します。
    * cats.dataの他の例としては、次の章で紹介するモナド変換器や、第6章で紹介するValidated型などがあります。
    *
    * 4.7.1 Creating and Unpacking Writers
    *
    * Writer[W, A]は、W型のログとA型の結果の2つの値を持ちます。
    * 以下のように、それぞれの型の値からWriterを作成することができます。
    */
  import cats.data.Writer
  import cats.instances.vector._ // for Monoid

  Writer(Vector(
           "It was the best of times",
           "it was the worst of times"
         ),
         1859)
  // res0: cats.data.WriterT[cats.package.Id, Vector[String], Int] = WriterT(
  //   (Vector("It was the best of times", "it was the worst of times"), 1859)
  // )

  /**
    * コンソールで報告される型は、予想通り Writer[Vector[String], Int]ではなく、実際には WriterT[Id, Vector[String], Int]であることに注意してください。
    * コードの再利用の精神から、Cats は Writer を別の型である WriterT で実装しています。
    * WriterT はモナド変換器と呼ばれる新しい概念の例で、次の章で説明します。
    *
    * この細かいことはとりあえず無視してみましょう。
    * WriterはWriterTの型のエイリアスなので、WriterT[Id, W, A]のような型をWriter[W, A]として読むことができます。
    */
  //  type Writer[W, A] = WriterT[Id, W, A]

  /**
    * 便利なように、Catsではログか結果のみを指定してライターを作成する方法を提供しています。
    * もし結果だけを持っている場合は、標準の純粋な構文を使うことができます。
    * これを行うには、スコープにMonoid[W]を持っていなければなりませんので、Catsは空のログを生成する方法を知っています。
    */
  import cats.instances.vector._ // for Monoid
  import cats.syntax.applicative._ // for pure

  //  type Logged[A] = Writer[Vector[String], A]
  //
  //  123.pure[Logged]
  //  // res1: Logged[Int] = WriterT((Vector(), 123))

  /**
    * ログがあっても結果が出ない場合は、cats.syntax.writer.writerのtell構文を使ってWriter[Unit]を作成することができます。
    */
  import cats.syntax.writer._ // for tell

  Vector("msg1", "msg2", "msg3").tell
  // res2: Writer[Vector[String], Unit] = WriterT(
  //   (Vector("msg1", "msg2", "msg3"), ())
  // )

  /**
    * 結果とログの両方を持っている場合は、Writer.applyを使うか、cats.syntax.writer.writerのライター構文を使うことができます。
    */
  import cats.syntax.writer._ // for writer

  //  val a = Writer(Vector("msg1", "msg2", "msg3"), 123)
  // a: cats.data.WriterT[cats.package.Id, Vector[String], Int] = WriterT(
  //   (Vector("msg1", "msg2", "msg3"), 123)
  // )
  //  val b = 123.writer(Vector("msg1", "msg2", "msg3"))
  // b: Writer[Vector[String], Int] = WriterT(
  //   (Vector("msg1", "msg2", "msg3"), 123)
  // )

  /**
    * ライターから結果とログを抽出するには、それぞれvalueメソッドとwrittenメソッドを使用します。
    */
  //  val aResult: Int =
  //    a.value
  //  // aResult: Int = 123
  //  val aLog: Vector[String] =
  //    a.written
  //  // aLog: Vector[String] = Vector("msg1", "msg2", "msg3")

  /**
    * runメソッドを使用して、両方の値を同時に抽出することができます。
    */
  //  val (log, result) = b.run
  //  // log: Vector[String] = Vector("msg1", "msg2", "msg3")
  //  // result: Int = 123

  /**
    * 4.7.2 Composing and Transforming Writers
    *
    * flatMap は、ソース Writer のログとユーザーのシーケンス関数の結果を追加します。
    * このため、Vectorのような効率的な追記・連結演算を持つログ型を使用するのが良いでしょう。
    */
  val writer1 = for {
    a <- 10.pure[Logged]
    _ <- Vector("a", "b", "c").tell
    b <- 32.writer(Vector("x", "y", "z"))
  } yield a + b
  // writer1: cats.data.WriterT[cats.package.Id, Vector[String], Int] = WriterT(
  //   (Vector("a", "b", "c", "x", "y", "z"), 42)
  // )
  //  writer1.run
  // res3: (Vector[String], Int) = (Vector("a", "b", "c", "x", "y", "z"), 42)

  /**
    * mapやflatMapで結果を変換するだけでなく、mapWrittenメソッドを使ってWriterでログを変換することもできます。
    */
  //  val writer2 = writer1.mapWritten(_.map(_.toUpperCase))
  // writer2: cats.data.WriterT[cats.package.Id, Vector[String], Int] = WriterT(
  //   (Vector("A", "B", "C", "X", "Y", "Z"), 42)
  // )

  //  writer2.run
  // res4: (Vector[String], Int) = (Vector("A", "B", "C", "X", "Y", "Z"), 42)

  /**
    * bimapは2つの関数のパラメータを取り、1つはログ用、もう1つは結果用に、mapBothは2つのパラメータを受け付ける1つの関数を取ります。
    */
  //  val writer3 = writer1.bimap(
  //    log => log.map(_.toUpperCase),
  //    res => res * 100
  //  )
  // writer3: cats.data.WriterT[cats.package.Id, Vector[String], Int] = WriterT(
  //   (Vector("A", "B", "C", "X", "Y", "Z"), 4200)
  // )

  //  writer3.run
  // res5: (Vector[String], Int) = (Vector("A", "B", "C", "X", "Y", "Z"), 4200)

  val writer4 = writer1.mapBoth { (log, res) =>
    val log2 = log.map(_ + "!")
    val res2 = res * 1000
    (log2, res2)
  }
  // writer4: cats.data.WriterT[cats.package.Id, Vector[String], Int] = WriterT(
  //   (Vector("a!", "b!", "c!", "x!", "y!", "z!"), 42000)
  // )

  //  writer4.run
  // res6: (Vector[String], Int) = (
  //   Vector("a!", "b!", "c!", "x!", "y!", "z!"),
  //   42000
  // )
  /**
    * 最後にリセット方式でログをクリアし、スワップ方式でログと結果をスワップします。
    */
  //  val writer5 = writer1.reset
  // writer5: cats.data.WriterT[cats.package.Id, Vector[String], Int] = WriterT(
  //   (Vector(), 42)
  // )

  //  writer5.run
  // res7: (Vector[String], Int) = (Vector(), 42)

  //  val writer6 = writer1.swap
  // writer6: cats.data.WriterT[cats.package.Id, Int, Vector[String]] = WriterT(
  //   (42, Vector("a", "b", "c", "x", "y", "z"))
  // )

  //  writer6.run
  // res8: (Int, Vector[String]) = (42, Vector("a", "b", "c", "x", "y", "z"))

  /**
    * 4.7.3 Exercise: Show Your Working
    *
    * ライターは、マルチスレッド環境での演算のロギングに便利です。いくつかの階乗を計算して（ロギングして）確認してみましょう。
    * 以下の階乗関数は、階乗を計算し、実行時に中間のステップを出力します。
    * Slowヘルパー関数は、以下の非常に小さな例でも実行に時間がかかることを保証します。
    */
//  def slowly[A](body: => A) =
//    try body
//    finally Thread.sleep(100)
//
//  def factorial(n: Int): Int = {
//    val ans = slowly(if (n == 0) 1 else n * factorial(n - 1))
//    println(s"fact $n $ans")
//    ans
//  }

  /**
    *
    * これは、単調に増加する値の連続した出力です。
    */
  //  println(factorial(5))
  // fact 0 1
  // fact 1 1
  // fact 2 2
  // fact 3 6
  // fact 4 24
  // fact 5 120
  // res9: Int = 120

  /**
    * いくつかの階乗を並列に開始すると、ログメッセージが標準的なoutでインターリーブされてしまうことがあります。
    * これにより、どのメッセージがどの計算から来たものかがわかりにくくなります。
    */
  import scala.concurrent._
  import scala.concurrent.ExecutionContext.Implicits._
  import scala.concurrent.duration._

  Await.result(Future.sequence(
                 Vector(
                   Future(factorial(5)),
                   Future(factorial(5))
                 )),
               5.seconds)
  // fact 0 1
  // fact 0 1
  // fact 1 1
  // fact 1 1
  // fact 2 2
  // fact 2 2
  // fact 3 6
  // fact 3 6
  // fact 4 24
  // fact 4 24
  // fact 5 120
  // fact 5 120
  // res: scala.collection.immutable.Vector[Int] =
  //   Vector(120, 120)

  /**
    * ライターでログ・メッセージをキャプチャするように階乗法を書き換えてください。
    * これにより、並行計算のログを確実に分離できることを実証してください。
    */
  import cats.data.Writer
  import cats.instances.vector._
  import cats.syntax.applicative._ // for pure

  type Logged[A] = Writer[Vector[String], A]
  42.pure[Logged]

  Vector("Message").tell

  import cats.instances.vector._ // for Monoid
  41.pure[Logged].map(_ + 1)

//  def factorial(n: Int): Logged[Int] =
//    for {
//      ans <- if (n == 0) {
//        1.pure[Logged]
//      } else {
//        slowly(factorial(n - 1).map(_ * n))
//      }
//      _ <- Vector(s"fact $n $ans").tell
//    } yield ans
//
//  val (log, res) = factorial(5).run
//
//  Await.result(Future
//                 .sequence(
//                   Vector(
//                     Future(factorial(5)),
//                     Future(factorial(5))
//                   ))
//                 .map(_.map(_.written)),
//               5.seconds)

  /**
    * 4.8 The Reader Monad
    *
    * cats.data.Readerは、いくつかの入力に依存する操作を連続して行うことを可能にするモナドです。
    * Readerのインスタンスは、1つの引数の関数をラップアップし、それらを構成するための便利なメソッドを提供してくれます。
    * Readerの一般的な使用法の一つに依存性の注入があります。
    * 外部の設定に依存する操作をいくつか持っている場合、リーダーを使ってそれらの操作を連鎖させることで、設定をパラメータとして受け取り、指定した順序でプログラムを実行する大きな操作を生成することができます。
    *
    * 4.8.1 Creating and Unpacking Readers
    *
    * 関数A => BからReader.applyコンストラクタを使ってReader[A, B]を作成します。
    */
  import cats.data.Reader

  final case class Cat(name: String, favoriteFood: String)

  val catName: Reader[Cat, String] =
    Reader(cat => cat.name)
  // catName: Reader[Cat, String] = Kleisli(<function1>)
  /**
    * 再びReaderのrunメソッドを使って関数を抽出し、いつものようにapplyを使って呼び出すことができます。
    */
  catName.run(Cat("Garfield", "lasagne"))
  // res1: cats.package.Id[String] = "Garfield"
  /**
    * ここまでは簡単ですが、リーダーはそのままの関数に比べてどのような利点があるのでしょうか？
    *
    * 4.8.2 Composing Readers
    * Readerの力は、異なる種類の関数の構成を表す map と flatMap メソッドにあります。
    * 一般的には、同じタイプのコンフィグレーションを受け入れるリーダーのセットを作成し、それらを map と flatMap で結合し、最後に run を呼び出してコンフィグレーションを注入します。
    *
    * mapメソッドは、その結果を関数に渡すことで、リーダーでの計算を単純に拡張します。
    */
  val greetKitty: Reader[Cat, String] =
    catName.map(name => s"Hello ${name}")

  greetKitty.run(Cat("Heathcliff", "junk food"))
  // res2: cats.package.Id[String] = "Hello Heathcliff"

  /**
    * flatMapメソッドは、より興味深いものです。これにより、同じ入力タイプに依存するリーダを組み合わせることができます。
    * これを説明するために、挨拶の例を拡張して猫にも餌を与えるようにしてみましょう。
    */
  val feedKitty: Reader[Cat, String] =
    Reader(cat => s"Have a nice bowl of ${cat.favoriteFood}")

  val greetAndFeed: Reader[Cat, String] =
    for {
      greet <- greetKitty
      feed <- feedKitty
    } yield s"$greet. $feed."

  greetAndFeed(Cat("Garfield", "lasagne"))
  // res3: cats.package.Id[String] = "Hello Garfield. Have a nice bowl of lasagne."
  greetAndFeed(Cat("Heathcliff", "junk food"))

  // res4: cats.package.Id[String] = "Hello Heathcliff. Have a nice bowl of junk food."

  /**
    * 4.8.3 Exercise: Hacking on Readers
    *
    * リーダーの古典的な使用法は、パラメータとして設定を受け入れるプログラムを構築することです。
    * ここでは、シンプルなログインシステムの完全な例で説明します。この設定は2つのデータベースで構成されています。
    *
    */
  final case class Db(
      usernames: Map[Int, String],
      passwords: Map[String, String]
  )

  /**
    * 入力として Db を消費するリーダーのために DbReader という型のエイリアスを作成することから始めます。
    * これにより、残りのコードを短くすることができます。
    */
  type DbReader[A] = Reader[Db, A]

  /**
    * 次に、IntユーザIDのユーザ名を検索するDbReadersを生成するメソッドと、Stringユーザ名のパスワードを検索するメソッドを作成します。
    * 型のシグネチャは以下のようにします。
    */
  import cats.syntax.applicative._
  def findUsername(userId: Int): DbReader[Option[String]] =
    Reader(db => db.usernames.get(userId))

  def checkPassword(username: String, password: String): DbReader[Boolean] =
    Reader(db => db.passwords.get(username).contains(password))

  def checkLogin(userId: Int, password: String): DbReader[Boolean] =
    for {
      user <- findUsername(userId)
      exist <- user
        .map { userName =>
          checkPassword(userName, password)
        }
        .getOrElse(false.pure[DbReader])
    } yield exist

  val users = Map(
    1 -> "dade",
    2 -> "kate",
    3 -> "margo"
  )

  val passwords = Map(
    "dade" -> "zerocool",
    "kate" -> "acidburn",
    "margo" -> "secret"
  )

  val db = Db(users, passwords)

  checkLogin(1, "zerocool").run(db)
  // res7: cats.package.Id[Boolean] = true
  checkLogin(4, "davinci").run(db)
  // res8: cats.package.Id[Boolean] = false

  /**
    * 4.8.4 When to Use Readers?
    *
    * リーダーは依存性注入を行うためのツールを提供します。
    * プログラムのステップをReaderのインスタンスとして記述し、mapやflatMapを使って連鎖させ、依存関係を入力として受け取る関数を構築します。
    * Scalaで依存性注入を実装する方法は、複数のパラメータリストを持つメソッド、暗黙のパラメータや型クラスなどのシンプルなものから、ケーキパターンやDIフレームワークのような複雑なものまで、さまざまな方法があります。
    * リーダは次のような状況で最も有用です。
    *
    * - 関数で簡単に表現できるプログラムを構築している。
    * - 既知のパラメータやパラメータセットの注入を延期する必要がある。
    * - プログラムの一部を個別にテストできるようにしたい。
    *
    * プログラムのステップをリーダーとして表現することで、純粋な関数のように簡単にテストでき、さらにマップやフラットマップ・コンビネータへのアクセスも可能になります。
    * 依存関係がたくさんあるような複雑な問題や、プログラムが純粋な関数として簡単に表現できないような問題では、他の依存関係注入技術の方が適切な傾向があります。
    *
    * Kleisli Arrows
    *
    * コンソール出力を見て、Reader が Kleisli と呼ばれる別の型で実装されていることに気づいたかもしれません。
    * Kleisli の矢印は、結果型のコンストラクタの型を一般化する Reader のより一般的な形式を提供する。
    * Kleislis には第 5 章で再び出会うことになるだろう。
    */
  /**
    * 4.9 The State Monad
    *
    * cats.data.Stateでは、計算の一部として追加のステートを渡すことができます。
    * アトミックな状態操作を表すStateインスタンスを定義し、mapとflatMapを使ってそれらをスレッド化します。
    * このようにして、実際の突然変異を使わずに、純粋に機能的な方法で突然変異可能な状態をモデル化することができます。
    *
    * 4.9.1 Creating and Unpacking State
    *
    * 最も単純な形で説明すると、State[S, A] のインスタンスは S => (S, A) 型の関数を表しています。S は状態の型、A は結果の型です。
    */
  import cats.data.State

  val a = State[Int, String] { state =>
    (state, s"The state is $state")
  }

  /**
    * 言い換えれば、Stateのインスタンスは2つのことを行う関数です。
    *
    * - 入力状態を出力状態に変換します。
    * - 結果を計算します。
    *
    * 初期状態を指定することで、モナドを「実行」することができます。
    * Stateには、状態と結果の組み合わせが異なる3つのメソッドrun、runS、runAが用意されています。各メソッドは、スタックの安全性を維持するために State が使用する Eval のインスタンスを返します。
    * 実際の結果を抽出するために、通常通り value メソッドを呼び出します。
    */
  // Get the state and the result:
//  val (state, result) = a.run(10).value
  // state: Int = 10
  // result: String = "The state is 10"

  // Get the state, ignore the result:
  val justTheState = a.runS(10).value
  // justTheState: Int = 10

  // Get the result, ignore the state:
  val justTheResult = a.runA(10).value
  // justTheResult: String = "The state is 10"

  /**
    * 4.9.2 Composing and Transforming State
    *
    * Reader と Writer で見てきたように、State モナドの力はインスタンスを組み合わせることで得られます。
    * mapメソッドとflatMapメソッドは、ステートをあるインスタンスから別のインスタンスにスレッドします。
    * 個々のインスタンスはアトミックな状態変換を表し、それらの組み合わせは一連の変化を完全に表します。
    */
  val step1 = State[Int, String] { num =>
    val ans = num + 1
    (ans, s"Result of step1: $ans")
  }

  val step2 = State[Int, String] { num =>
    val ans = num * 2
    (ans, s"Result of step2: $ans")
  }

  val both = for {
    a <- step1
    b <- step2
  } yield (a, b)

  val (state, result) = both.run(20).value
  // state: Int = 42
  // result: (String, String) = ("Result of step1: 21", "Result of step2: 42")

  /**
    * ご覧のように、この例では、最終的な状態は、両方の変換を順番に適用した結果です。
    * 状態は、理解のための対話をしていないにもかかわらず、ステップからステップへとスレッド化されています。
    *
    * Stateモナドを使用するための一般的なモデルは、計算の各ステップをインスタンスとして表現し、標準のモナド演算子を使用してステップを構成することです。
    * Catsは、プリミティブステップを作成するための便利なコンストラクタをいくつか提供しています。
    *
    * - get は状態を結果として抽出します。
    * - set は状態を更新し、結果として単位を返します。
    * - pure は状態を無視し、与えられた結果を返します。
    * - inspect は、変換関数を使って状態を抽出します。
    * - modify は、更新関数を使用して状態を更新します。
    */
  val getDemo = State.get[Int]
  // getDemo: State[Int, Int] = cats.data.IndexedStateT@741518c8
  getDemo.run(10).value
  // res1: (Int, Int) = (10, 10)

  val setDemo = State.set[Int](30)
  // setDemo: State[Int, Unit] = cats.data.IndexedStateT@509fb0a
  setDemo.run(10).value
  // res2: (Int, Unit) = (30, ())

  val pureDemo = State.pure[Int, String]("Result")
  // pureDemo: State[Int, String] = cats.data.IndexedStateT@562ae0a8
  pureDemo.run(10).value
  // res3: (Int, String) = (10, "Result")

  val inspectDemo = State.inspect[Int, String](x => s"${x}!")
  // inspectDemo: State[Int, String] = cats.data.IndexedStateT@2dc6b50f
  inspectDemo.run(10).value
  // res4: (Int, String) = (10, "10!")

  val modifyDemo = State.modify[Int](_ + 1)
  // modifyDemo: State[Int, Unit] = cats.data.IndexedStateT@71c93b27
  modifyDemo.run(10).value
  // res5: (Int, Unit) = (11, ())

  /**
    * これらの構成要素は、組み立てることにより理解することができる。
    * 私たちは通常、状態の変換のみを表す中間段階の結果を無視します。
    */
  import cats.data.State
  import State._

  val program: State[Int, (Int, Int, Int)] = for {
    a <- get[Int]
    _ <- set[Int](a + 1)
    b <- get[Int]
    _ <- modify[Int](_ + 1)
    c <- inspect[Int, Int](_ * 1000)
  } yield (a, b, c)
  // program: State[Int, (Int, Int, Int)] = cats.data.IndexedStateT@3b525fbf

//  val (state, result) = program.run(1).value
  // state: Int = 3
  // result: (Int, Int, Int) = (1, 2, 3000)

  /**
    * 4.9.3 Exercise: Post-Order Calculator
    * Stateモナドを使用すると、複雑な式のためのシンプルなインタプリタを実装することができ、結果と一緒にミュータブルレジスタの値を渡すことができます。
    * 次の整数演算式のための計算機を実装することで、簡単な例を見ることができます。
    *
    * ポストオーダー式を聞いたことがない方のために説明しておきますが（聞いたことがなくても大丈夫です）、これは数学的な記法で、演算子のオペランドの後に演算子を記述します。
    * ですから、例えば、1 + 2 と書く代わりに、次のように書きます。
    *
    * 1 2 +
    *
    * 後置式は人間が読むのは難しいですが、コードで評価するのは簡単です。必要なのは、記号を左から右になぞって、オペランドのスタックを持って行くことだけです。
    * 数字が見えたら、それをスタックにプッシュします。
    * 演算子を見たら、2つのオペランドをスタックから取り出して、それらを操作し、結果をその場所にプッシュします。
    * これにより、括弧を使わずに複雑な式を評価することができます。例えば、(1 + 2) * 3)を次のように評価することができます。
    *
    * 1 2 + 3 * // see 1, push onto stack
    * 2 + 3 *   // see 2, push onto stack
    * + 3 *     // see +, pop 1 and 2 off of stack,
    * // push (1 + 2) = 3 in their place
    * 3 3 *     // see 3, push onto stack
    * 3 *       // see 3, push onto stack
    * * // see *, pop 3 and 3 off of stack,
    * //        push (3 * 3) = 9 in their place
    *
    * これらの式のインタプリタを書いてみましょう。各シンボルを、スタック上の変換と中間結果を表すステートインスタンスにパースすることができます。
    * ステートインスタンスは flatMap を使ってスレッド化して、シンボルのシーケンスに対応するインタプリタを作成することができます。
    *
    * 1つのシンボルを解析してStateのインスタンスに変換する関数evalOneを書くことから始めましょう。
    * 以下のコードをテンプレートとして使用してください。スタックの設定が間違っている場合は例外を投げても構いません。
    */
  import cats.data.State

  type CalcState[A] = State[List[Int], A]

  // 答え見た
  def evalOne(sym: String): CalcState[Int] = sym match {
    case "+" => operator(_ + _)
    case "-" => operator(_ - _)
    case "*" => operator(_ * _)
    case "/" => operator(_ / _)
    case num => operand(num.toInt)
  }

  def operand(num: Int): CalcState[Int] =
    State[List[Int], Int] { stack =>
      (num :: stack, num)
    }

  def operator(func: (Int, Int) => Int): CalcState[Int] =
    State[List[Int], Int] {
      case b :: a :: tail =>
        val ans = func(a, b)
        (ans :: tail, ans)

      case _ =>
        sys.error("Fail!")
    }

  /**
    * これが難しいようであれば、返すステートインスタンスの基本的な形を考えてみましょう。
    * 各インスタンスは、スタックからスタックと結果のペアへの機能的な変換を表しています。
    * より広い文脈を無視して、その一歩だけに集中することができます。
    */
//  State[List[Int], Int] { oldStack =>
//    val newStack = someTransformation(oldStack)
//    val result = someCalculation
//    (newStack, result)
//  }

//  def someTransformation(l: List[Int]) = ???
//  def someCalculation = ???

  /**
    * スタックインスタンスをこの形式で書いても、先ほどの便利なコンストラクタのシーケンスとして書いても構いません。
    *
    * evalOne を使用すると，次のようにして単一シンボルの式を評価することができます．
    * runA を呼び出して Nil を初期スタックとして与え、value を呼び出して結果の Eval インスタンスをアンパックします。
    */
  evalOne("42").runA(Nil).value
  // res10: Int = 42

  /**
    * より複雑なプログラムを表現するには，evalOne，map，flatMap を使用します．
    * なお，ほとんどの作業はスタック上で行われているので，evalOne("1")とevalOne("2")の中間ステップの結果は無視しています．
    */
//  val program = for {
//    _ <- evalOne("1")
//    _ <- evalOne("2")
//    ans <- evalOne("+")
//  } yield ans
//  // program: cats.data.IndexedStateT[cats.Eval, List[Int], List[Int], Int] = cats.data.IndexedStateT@3afcc7dd
//
//  program.runA(1).value
  // res11: Int = 3

  /**
    * この例を一般化して、List[String]の結果を計算するevalAllメソッドを書きます。
    * 各シンボルの処理には evalOne を使用し，結果の State モナドを flatMap でつなぎます．
    * この関数は次のようなシグネチャを持つ必要があります．
    */
  import cats.syntax.applicative._ // for pure
  def evalAll(input: List[String]): CalcState[Int] =
    input.foldLeft(0.pure[CalcState]) { (a, b) =>
      a.flatMap(_ => evalOne(b))
    }

  /**
    * evalAllを使えば、多段式の評価を便利に行うことができます。
    */
  val multistageProgram = evalAll(List("1", "2", "+", "3", "*"))
  // multistageProgram: CalcState[Int] = cats.data.IndexedStateT@228a6340

  multistageProgram.runA(Nil).value
  // res13: Int = 9

  /**
    * evalOne と evalAll はどちらも State のインスタンスを返すので，flatMap を使ってこれらの結果を連結することができます．
    * evalOne は単純なスタック変換を行い，evalAll は複雑な変換を行いますが，どちらも純粋な関数なので，好きな順序で何度でも使用することができます．
    */
  val biggerProgram = for {
    _ <- evalAll(List("1", "2", "+"))
    _ <- evalAll(List("3", "4", "+"))
    ans <- evalOne("*")
  } yield ans
  // biggerProgram: cats.data.IndexedStateT[cats.Eval, List[Int], List[Int], Int] = cats.data.IndexedStateT@1a227435

  biggerProgram.runA(Nil).value
  // res14: Int = 21

  /**
    * 入力された文字列をシンボルに分割し、evalAllを呼び出し、初期スタックで結果を実行するevalInput関数を実装して、演習を完了します。
    */
  def evalInput(l: String): Int =
    evalAll(l.split(" ").toList).runA(Nil).value

  evalInput("1 2 + 3 4 + *")

  /**
    * 4.10 Defining Custom Monads
    *
    * フラットマップ、ピュア、そしてまだ見ぬメソッドであるtailRecMの3つのメソッドの実装を用意することで、カスタムタイプのモナドを定義することができます。
    * ここでは、例としてOptionに対するMonadの実装を紹介します。
    */
  import scala.annotation.tailrec

  val optionMonad = new Monad[Option] {
    def flatMap[A, B](opt: Option[A])(fn: A => Option[B]): Option[B] =
      opt flatMap fn

    def pure[A](opt: A): Option[A] =
      Some(opt)

    @tailrec
    def tailRecM[A, B](a: A)(fn: A => Option[Either[A, B]]): Option[B] =
      fn(a) match {
        case None           => None
        case Some(Left(a1)) => tailRecM(a1)(fn)
        case Some(Right(b)) => Some(b)
      }
  }

  /**
    * tailRecM法は、Catsで使われている最適化で、flatMapのネストされた呼び出しで消費されるスタックスペースの量を制限するものです。
    * この手法は、PureScriptの生みの親であるPhil Freeman氏が2015年に発表した論文に由来します。
    * このメソッドは、fnの結果がRightを返すまで、自分自身を再帰的に呼び出す必要があります。
    *
    * その使い方を動機付けるために、次のような例を使ってみましょう。
    * 関数が停止すべきだと示すまで、関数を呼び出すメソッドを書きたいとします。
    * 関数はモナドのインスタンスを返します。
    * なぜなら、モナドはシーケンスを表し、多くのモナドは停止の概念を持っているからです。
    *
    * このメソッドは、flatMapで記述することができます
    */
//  import cats.syntax.flatMap._ // For flatMap
//
//  def retry[F[_]: Monad, A](start: A)(f: A => F[A]): F[A] =
//    f(start).flatMap { a =>
//      retry(a)(f)
//  }

//  import cats.instances.option._
//
//  retry(100)(a => if(a == 0) None else Some(a - 1))
//  // res1: Option[Int] = None
//
//  retry(100000)(a => if(a == 0) None else Some(a - 1))
//  // KABLOOIE!!!!

  /**
    * 代わりにtailRecMを使ってこのメソッドを書き換えることができます。
    */
//  def retryTailRecM[F[_]: Monad, A](start: A)(f: A => F[A]): F[A] =
//    Monad[F].tailRecM(start){ a =>
//      f(a).map(a2 => Left(a2))
//    }

  /**
    * 今では何度recurseしても正常に動作するようになりました。
    */
//  retryTailRecM(100000)(a => if(a == 0) None else Some(a - 1))
//  // res2: Option[Int] = None

  /**
    * ここで重要なのは、明示的にtailRecMを呼び出さなければならないということです。
    * tailRecMを使用する非tail再帰的コードをtail再帰的コードに変換するコード変換はありません。
    * しかし、モナド型クラスが提供するいくつかのユーティリティがあり、この種のメソッドを簡単に書くことができます。
    * たとえば、retryをiterateWhileMの観点から書き換えれば、tailRecMを明示的に呼び出す必要はありません。
    */
//  def retryM[F[_]: Monad, A](start: A)(f: A => F[A]): F[A] =
//    start.iterateWhileM(f)(a => true)
//
//  retryM(100000)(a => if (a == 0) None else Some(a - 1))
//  // res3: Option[Int] = None

  /**
    * 私達はtailRecMをセクション7.1で見ていくことになるでしょう
    *
    * CatsのすべてのビルトインモナドはtailRecMの尾再帰的な実装を持っていますが、カスタムモナドのためにそれを書くのは難しいことです...これから見ていきましょう。
    *
    * 4.10.1 Exercise: Branching out Further with Monads
    *
    * 前章で紹介したTreeデータ型のモナドを書いてみましょう。データ型は次のとおりです。
    */
  sealed trait Tree[+A]

  final case class Branch[A](left: Tree[A], right: Tree[A]) extends Tree[A]

  final case class Leaf[A](value: A) extends Tree[A]

  def branch[A](left: Tree[A], right: Tree[A]): Tree[A] =
    Branch(left, right)

  def leaf[A](value: A): Tree[A] =
    Leaf(value)

  /**
    * コードがBranchとLeafのインスタンス上で動作し、モナドがFunctorのような動作を無料で提供していることを確認します。
    *
    * また、TreeにflatMapやmapを直接実装していないにもかかわらず、スコープ内にMonadを持つことで内包を使用できることも確認してください。
    *
    * tailRecMをtail-recursiveにしなければならないと思わないでください。
    * それはとても難しいことです。
    * ソリューションには末尾再帰的な実装とそうでない実装の両方が含まれていますので、自分の作業を確認することができます。
    */
  /**
    * excersize
    */
  implicit val treeMonad = new Monad[Tree] {
    def pure[A](value: A): Tree[A] =
      Leaf(value)

    def flatMap[A, B](tree: Tree[A])(func: A => Tree[B]): Tree[B] =
      tree match {
        case Branch(l, r) =>
          Branch(flatMap(l)(func), flatMap(r)(func))
        case Leaf(value) =>
          func(value)
      }

    def tailRecM[A, B](a: A)(func: A => Tree[Either[A, B]]): Tree[B] = {
      // flatMapの中が末尾再帰になっているだけなので、末尾再帰じゃない
      flatMap(func(a)) {
        case Left(value) =>
          tailRecM(value)(func)
        case Right(value) =>
          Leaf(value)
      }
    }
  }

  /**
  * 4.11 Summary
  *
  * この章では、モナドを間近で見てきました。
  * flatMapは、計算の順序を決める演算子と見ることができ、演算の順番を決めることができます。
  * この観点から、Optionはエラーメッセージを出さずに失敗する可能性のある計算を、
  * Eitherはメッセージを出して失敗する可能性のある計算を、Listは複数の可能性のある結果を、
  * Futureは将来のある時点で値を生成する可能性のある計算を表します。
  *
  * また、Catsが提供するId、Reader、Writer、Stateなどのカスタムタイプやデータ構造も見てきました。
  * これらは幅広いユースケースをカバーしています。
  *
  * 最後に、万が一カスタムモナドを実装しなければならない場合に備えて、tailRecMを使って独自のインスタンスを定義する方法を学びました。
  * tailRecMは、デフォルトでスタックセーフな関数型プログラミングライブラリを構築するための譲歩とも言える、ちょっと変わった仕組みです。
  * モナドを理解するためにtailRecMを理解する必要はありませんが、tailRecMがあることで、モナドのコードを書くときに感謝できるメリットがあります。
  */

}
