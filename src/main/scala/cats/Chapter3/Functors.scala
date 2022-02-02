package cats.Chapter3

object Functors extends App {

  // 3 Functors
  /**
    * この章では、リストやオプションなどのコンテキスト内で一連の操作を表現するための抽象化であるファンクタについて調べていきます。
    * ファンクタ単体ではそれほど便利ではありませんが、monadsやapplicative functorsのようなファンクタの特殊なケースなどは、
    * Catsで最もよく使われる抽象概念です。
    */
  // 3.1 Examples of Functors
  /**
    * 非公式には、Functorはマップメソッドを持つものです。
    * おそらく、これを持つタイプをたくさん知っていると思います。いくつか例を上げると、Option、List、Eitherなどです。
    * 通常、リストを反復処理するときに最初にMapにを使うでしょう。
    * しかし、ファンクタを理解するためには、別の方法でメソッドを考える必要があります。
    * リストを横断(1つ1つ反復して処理しているのではなく)するのではなく、内部のすべての値を一度に変換すると考えるべきです。
    * 適用する関数を指定し、マップはそれが全ての項目に適用されることを保証します。
    * 値は変わりますが、リストの構造（要素の数と順序）は変わりません。
    */
  //  List(1, 2, 3).map(n => n + 1)
  // res0: List[Int] = List(2, 3, 4)

  /**
    * 同様に、Option にMapするときは、中身は変換しますが、Some または None のコンテキストは変更しません。
    * 同じ原理は、LeftとRightのコンテキストを持つ Either にも適用されます。
    * この変換の一般的な概念は、図1に示されている型のシグネチャの共通パターンとともに、異なるデータ型にまたがるマップの動作を結びつけています。
    *
    * == 図1 ==
    *
    * mapはコンテキストの構造を変更しないため、この関数を繰り返し呼び出し、
    * 初期データ構造の内容に複数の計算を連続して行うことができます。
    */
//  List(1, 2, 3).map(n => n + 1).map(n => n * 2).map(n => s"${n}!")
  // 1. List(2, 3, 4)
  // 2. List(4, 6, 8)
  // 3. List("4!", "6!", "8!")
  // res1: List[String] = List("4!", "6!", "8!")

  /**
    * マップは反復パターンとしてではなく、関連するデータ型によって指定された複雑さを無視して、
    * 値に対する計算を順次行う方法として考えるべきです。
    * - Option　- 値が存在する場合と存在しない場合があります。
    * - Either　- 値またはエラーがある可能性があります。
    * - List    - 値が0個以上ある場合があります。
    */
  // 3.2 More Examples of Functors
  /**
    * List, Option, Either のマップメソッドは一貫して関数を適用します。
    * しかし、計算を順次行うという考え方は、これよりももっと一般的です。
    * パターンを異なる方法で適用する他のいくつかのファンクタの振る舞いを調べてみましょう。
    */
  // Futures
  /**
    * Futureは、非同期計算をキューイングし、
    * 前処理の完了に合わせて適用することで、非同期計算をシーケンスするファンクタです。
    * 図2に示されているそのマップメソッドの型シグネチャは、上記のシグネチャと同じ形をしています。
    * しかし、挙動は大きく異なります。
    *
    * == 図2 ==
    *
    * Futureを扱う際には、その内部状態を保証するものではありません。
    * ラップされた計算は、進行中であったり、完了していたり、拒否されていたりします。
    * フューチャーが完了していれば、マッピング関数をすぐに呼び出すことができます。
    * そうでない場合は、基礎となるスレッドプール(スレッドプールというのは、複数のスレッドをあらかじめ作成して待機させておき、タスクが来たら待っているスレッドにタスクを割り当てて処理を開始させる、という仕組みのことをいいます。)
    * が関数呼び出しをキューに入れ、後で再処理されます。
    * 私達は関数がいつ呼び出されるかはわかりませんが、どのような順序で呼び出されるかはわかります。
    * このようにして、Future は List、Option、Either で見られるのと同じシーケンス動作を提供します。
    */
//  import scala.concurrent.{Future, Await}
//  import scala.concurrent.ExecutionContext.Implicits.global
//  import scala.concurrent.duration._
//
//  val future: Future[String] =
//    Future(123).
//      map(n => n + 1).
//      map(n => n * 2).
//      map(n => s"${n}!")
//
//  Await.result(future, 1.second)
//   res2: String = "248!"

//   Futures and Referential Transparency

  /**
    * ScalaのFuturesは、参照透過的でないため純粋な関数型プログラミングの良い例ではないことに注意してください。
    * Futureは常に結果を計算してキャッシュしており、この振る舞いを微調整することはできません。
    * つまり、Futureを使って副作用のある計算をラップすると、予測できない結果が得られる可能性があるということです。
    * 例えば、以下のようになります。
    */
//  import scala.concurrent.{Future, Await}
//  import scala.concurrent.ExecutionContext.Implicits.global
//  import scala.util.Random
//  import scala.concurrent.duration._
//
//  val future1 = {
//    // Initialize Random with a fixed seed:
//    val r = new Random(0L)
//
//    // nextInt は、シーケンス内の次の乱数に移動するという副作用があります。
//    val x = Future(r.nextInt)
//
//    for {
//      a <- x
//      b <- x
//    } yield (a, b)
//  }
//
//  val future2 = {
//    val r = new Random(0L)
//
//    for {
//      a <- Future(r.nextInt)
//      b <- Future(r.nextInt)
//    } yield (a, b)
//  }
//
//  val result1 = Await.result(future1, 1.second)
//  println(result1)
//  // result1: (Int, Int) = (-1155484576, -1155484576)
//  val result2 = Await.result(future2, 1.second)
//  println(result2)
//  // result2: (Int, Int) = (-1155484576, -723955400)

  /**
    * result1とresult2が同じ値を持つことが理想的です。
    * しかし、future1の計算は1回nextIntを呼び出し、future2の計算はnextIntを2回呼び出します。
    * nextIntは毎回異なる結果を返すので、それぞれのケースで異なる結果が得られることになります。
    * このような矛盾は、Futureや副作用を含むプログラムについての論理を難しくしています。
    * また、Futureの動作には他にも問題のある側面があります。
    * 例えば、ユーザーがプログラムをいつ実行するかを指定するのではなく、常にすぐに計算を開始することなどです。
    * 詳細については、Rob Norris氏によるRedditの素晴らしい回答を参照してください。
    * Cats Effectを見ると、IO型がこれらの問題を解決していることがわかります。
    */
  /**
    * もしFutureが参照的透過的でないならば、おそらく別の似たようなデータ型を見てみるべきことに気づくべきです...
    */
  // Functions (?!)
  /**
    *  単一引数の関数もファンクタであることがわかります。
    *  単一引数の関数もファンクタであことを確認するためには、型を少し調整する必要があります。
    *  関数A => Bには2つの型のパラメータがあります：パラメータ型Aと結果型Bです。
    *  これらを強制的に正しい形にするために、パラメータの型を固定し、結果の型を変化させることができます。
    *  − X => A; から始めて、
    *  - A => B;　左記のfunctionを設定します
    *  - X => B. を受け取ります。
    *
    *  X => A を MyFunc[A] としてエイリアスすると、この章の他の例で見たのと同じ型のパターンが見えます。
    *  これは図3でも見られます。
    *  - MyFunc[A]; から始まります
    *
    *  - MyFunc[A] から始め、
    *  - A => B 左記のfunctionを設定します
    *  - MyFunc[B] を返します。
    *
    *  ==図3==
    *
    *  言い換えれば、Function1の上に「マッピング」することが関数合成です。
    */
//  import cats.instances.function._ // for Functor
//  import cats.syntax.functor._ // for map
//
//  val func1: Int => Double =
//    (x: Int) => x.toDouble
//
//  val func2: Double => Double =
//    (y: Double) => y * 2
////
//  println((func1 map func2)(1)) // mapを使って構成
//////   res3: Double = 2.0
//  println((func1 andThen func2)(1)) // andThenを使って構成
////  // res4: Double = 2.0
//  println(func2(func1(1))) // 手動で記載
//  // res5: Double = 2.0
  //

  /**
    * これは、一般的なパターンであるシーケンシング操作とどのように関係しているのでしょうか？
    * 関数の構成はシーケンスです。
    * 1つの操作を実行する関数から開始し、map を使用するたびに別の操作をチェーンに追加します。
    * map を呼び出しても実際にはどの操作も実行されませんが、最終関数に引数を渡すことができれば、すべての操作が順番に実行されます。
    * これは、Futureと同じように、操作をlazilyキューに入れていると考えることができます。
    */
//  import cats.instances.function._ // for Functor
//  import cats.syntax.functor._ // for map
//  val func =
//    ((x: Int) => x.toDouble).map(x => x + 1).map(x => x * 2).map(x => s"${x}!")
//
//  println(func(123))
//    //res6: String = "248.0!"

  // 部分統一
  /**
    * 上記の例を動作させるためには、Scala 2.13以前のバージョンでは、以下のコンパイラオプションをbuild.sbtに追加する必要があります。
    * scalacOptions += "-Ypartial-unification"
    * そうでなければ、コンパイラエラーが起こります。
    *　func1.map(func2)
    * <console>: error: value map is not a member of Int => Double
    * なぜこのようなことが起こるのかについては、3.7節で詳しく見ていきましょう。
    */
  // 3.3 Definition of a Functor

  /**
    * これまで見てきた例はすべてファンクタです。
    * シーケンシング計算をカプセル化したクラスです。
    * 形式的には，ファンクタは F[A] の型と，型 (A => B) => F[B] の演算マップを持つものです。
    * 一般的な型図を図4に示します．
    *
    * ==図4===
    *
    * CatsはFunctorをcats.Functorという型のクラスとしてエンコードしているので、メソッドの見た目は少し変わっています。
    * このメソッドは、変換関数と一緒に初期値F[A]をパラメータとして受け取ります。
    * 以下は定義を簡略化したものです。
    */
//  trait Functor[F[_]] {
//    def map[A, B](fa: F[A])(f: A => B): F[B]
//  }

  /**
    * もし今までF[_]のようなシンタックスを見たことがなければ、
    * 型のコンストラクタや上位の型について議論するために、簡単な回り道をしてみましょう。
    */
  // Functor Laws
  /**
    * 関数は、多くの小さな操作を一つずつ順番に並べていく場合でも、マッピングする前に大きな関数に結合していく場合でも、同じセマンティクスを保証します。
    * これを保証するためには、以下の法則が満たされていなければなりません。
    *
    * Identity: mapをidentityファンクションとともに呼び出すのは何もしないのと同じです。
    */
//  fa.map(a => a) == fa

  /**
    * 構成：fとgの2つの関数でmappingすることは、fでmappingした後にgでmappingすることと同じです。
    */
//  fa.map(g(f(_))) == fa.map(f).map(g)

  // 3.4 Aside: Higher Kinds and Type Constructors

  /**
    * Kindsはタイプのためのタイプのようなものです。
    * これらは型の中の「穴」の数を記述します。
    * 穴のない通常の型と、穴を埋めて型を生成する「型コンストラクタ」を区別しています。
    * 例えば、Listは1つの穴が開いている型のコンストラクタです。
    * List[Int]やList[A]のような正規型を生成するためのパラメータを指定することで、その穴を埋めます。
    * 型コンストラクタと汎用型を混同しないようにするのがコツです。Listは型コンストラクタであり、List[A]は型です。
    */
//  List    // 1つのパラメータを取る型コンストラクション
//  List[A] // 型は、型パラメータを適用して生成される。

  /**
    * ここでは、関数と値の類似点があります。
    * 関数は「値のコンストラクタ」であり、パラメータを与えると値を生成します。
    */
//  math.abs    // 1つのパラメータを取るファンクション
//  math.abs(x) // パラメータを適用して生成された値

  /**
    * Scalaではアンダースコアを使って型コンストラクタを宣言します。
    * これは型のコンストラクタが何個の「穴」を持っているかを指定するものです。
    * ただし、これを使う場合は名前だけを参照します。
    */
//  // Declare F using underscores:
//  def myMethod[F[_]] = {
//
//    // Reference F without underscores:
//    val functor = Functor.apply[F]
//
//    // ...
//  }

  /**
    * これは、関数のパラメータの型を指定するのと似ています。
    * パラメータを宣言するときには、その型も指定します。
    * ただし、これを使う場合は名前だけを参照します。
    */
  // Declare f specifying parameter types
//  def f(x: Int): Int =
  // Reference x without type
//    x * 2

  /**
    * 型コンストラクタの知識があれば、CatsのFunctorの定義では、List、Option、Future、
    * またはMyFuncのような型のエイリアスのような単一パラメータの型コンストラクタのインスタンスを作成することができることがわかります。
    */
  // Language Feature Imports
  /**
    * 2.13以前のバージョンのScalaでは、
    * A[_]構文を持つ型コンストラクタを宣言したときにコンパイラからの警告を抑制するために、より高い種類の型言語機能を「有効」にする必要があります。
    * この機能を有効にするには、上記のように "言語のインポート "を行う必要があります。
    */
  // import scala.language.higherKinds
  /**
    * または build.sbt.の scalacOptions に以下を追加してください。
    */
  // または build.sbt.の scalacOptions に以下を追加してください。
  /**
    * 実際には scalacOptions フラグが 2 つのオプションのうち単純なものであることがわかります。
    */
  // 2020/01/12
  // 3.5 Functors in Cats

  /**
    * Catsでのファンクタの実装を見てみましょう。
    * モノイドで行ったのと同じように、型クラス、インスタンス、構文を見ていきます。
    */
  // 3.5.1 The Functor Type Class and Instances

  /**
    * ファンクタ型のクラスは cats.Functor です。
    * コンパニオンオブジェクト上の標準的なFunctor.applyメソッドを使ってインスタンスを取得しています。
    * いつものように、デフォルトのインスタンスは cats.instances パッケージの中のタイプ別に並べられています。
    */
  import cats.Functor
//  import cats.instances.option._ // for Functor
//  import cats.instances.list._ // for Functor
////
//  val list1 = List(1, 2, 3)
//  // list1: List[Int] = List(1, 2, 3)
//  val list2 = Functor[List].map(list1)(_ * 2)
//  // list2: List[Int] = List(2, 4, 6)
//
//  val option1 = Option(123)
//  // option1: Option[Int] = Some(123)
//  val option2 = Functor[Option].map(option1)(_.toString)
//  // option2: Option[String] = Some("123")
//
//  /**
//    * ファンクタは lift と呼ばれるメソッドを提供しています。
//    * これは、ファンクタ上で動作し、A => B 型の関数を、 F[A] => F[B] 型を持つ関数に変換します。
//    */
//
//  val func = (x: Int) => x + 1
//  // func: Int => Int = <function1>
//
//  val liftedFunc = Functor[Option].lift(func)
//  // liftedFunc: Option[Int] => Option[Int] = cats.Functor$$Lambda$11546/665425203@13439aca
//
//  liftedFunc(Option(1))
//  // res1: Option[Int] = Some(2)
//
//  /**
//    * asメソッドは、使用する可能性が高いメソッドです。。
//    * ファンクタ内の値を与えられた値に置き換えます。
//    */
//  Functor[List].as(list1, "As")
  // res2: List[String] = List("As", "As", "As")

//  // 3.5.2 Functor Syntax
  /**
    * Functorの構文で提供される主なメソッドはmapです。
    * オプションやリストには独自のマップメソッドが組み込まれており、
    * Scalaコンパイラは常に拡張メソッドよりも組み込みメソッドを好むため、これを実演するのは困難です。
    * この問題を回避するために、2つの例を使ってみましょう。
    * まず、関数へのマッピングを見てみましょう。
    * ScalaのFunction1型にはマップメソッドがないので（代わりにandThenと呼ばれています）、名前の衝突はありません。
    */
//  val func1 = (a: Int) => a + 1
//  val func2 = (a: Int) => a * 2
//  val func3 = (a: Int) => s"${a}!"
//  val func4 = func1.map(func2).map(func3)
//
//  func4(123)
//  // res3: String = "248!"

  /**
    * 別の例を見てみましょう。今回はファンクタを抽象化して、特定の具体的な型を使わないようにします。
    * ファンクタのコンテキストに関係なく、数式を数値に適用するメソッドを書くことができます。
//    */
//  def doMath[F[_]](start: F[Int])
//                  (implicit functor: Functor[F]): F[Int] =
//    start.map(n => n + 1 * 2)
//
//  doMath(Option(20))
//  // res4: Option[Int] = Some(22)
//  doMath(List(1, 2, 3))
//  // res5: List[Int] = List(3, 4, 5)

  /**
    * これがどのように動作するかを説明するために、cats.syntax.functorのmapメソッドの定義を見てみましょう。
    * ここではコードを簡略化しています。
    */
//  implicit class FunctorOps[F[_], A](src: F[A]) {
//    def map[B](func: A => B)(implicit functor: Functor[F]): F[B] =
//      functor.map(src)(func)
//  }

  /**
    * コンパイラはこの拡張メソッドを使用して、組み込みのマップが利用できない場所にマップ・メソッドを挿入することができます。
    */
//  foo.map(value => value + 1)

  /**
    * foo にはマップメソッドが組み込まれていないと仮定すると、
    * コンパイラは潜在的なエラーを検出し、コードを修正するために FunctorOps で式をラップします。
    */
//  new FunctorOps(foo).map(value => value + 1)

  /**
    * FunctorOps の map メソッドは、暗黙の Functor をパラメータとして必要とします。
    * つまり、このコードはスコープ内に F 用の Functor がある場合にのみコンパイルされます。
    * ない場合はコンパイラエラーになります。
    */
//  final case class Box[A](value: A)
//
//  val box = Box[Int](123)
//
//  box.map(value => value + 1)
  // error: value map is not a member of repl.Session.App0.Box[Int]
//   box.map(value => value + 1)
  // ^^^^^^^

  // asメソッドは構文としても利用できます。
//  List(1, 2, 3).as("As")
  // res7: List[String] = List("As", "As", "As")

  // 3.5.3 Instances for Custom Types

  import cats.Functor
  import scala.concurrent.{Future, ExecutionContext}

  /**
    * ファンクタはマップメソッドを定義するだけで定義できます。
    * ここに Option 用のファンクタの例があります。
    * 実装は簡単で、Optionのマップメソッドを呼び出すだけです。
    */
  implicit val optionFunctor: Functor[Option] =
    new Functor[Option] {
      def map[A, B](value: Option[A])(func: A => B): Option[B] =
        value.map(func)
    }

  /**
    * インスタンスに依存関係を注入する必要があることもあります。
    * 例えば、Future用のカスタムFunctorを定義する必要がある場合（別の仮定の例-Catsはcats.instance.futureで提供しています）
    * 、future.mapの暗黙のExecutionContextパラメータを考慮する必要があります。
    * functor.mapに追加のパラメータを追加することはできないので、インスタンスを作成する際に依存関係を考慮しなければなりません。
    */
  implicit def futureFunctor(implicit ec: ExecutionContext): Functor[Future] =
    new Functor[Future] {
      def map[A, B](value: Future[A])(func: A => B): Future[B] =
        value.map(func)
    }

  /**
    * Functor.apply を直接使用して、または map 拡張メソッドを介して間接的に
    * Functor for Future を召喚すると、コンパイラは暗黙の解決によって futureFunctor を見つけ、
    * 呼び出し先の ExecutionContext を再帰的に検索します。このような展開になるかもしれません。
    */
//  import ExecutionContext.Implicits.global
//  val executionContext = ???
////  // We write this:
//  Functor[Future]
////
////  // The compiler expands to this first:
//  Functor[Future](futureFunctor)
////
////  // And then to this:
//  Functor[Future](futureFunctor(executionContext))

  // 3.5.4 Exercise: Branching out with Functors

  /**
    * 次のバイナリ・ツリー・データ型用のファンクタを書いてください。
    * このコードが、Branch と Leaf のインスタンスで期待通りに動作することを確認してください。
    */
//  sealed trait Tree[+A]
//
//  final case class Branch[A](left: Tree[A], right: Tree[A]) extends Tree[A]
//
//  final case class Leaf[A](value: A) extends Tree[A]
  /**
    * セマンティクスは、List用のFunctorを書くのと似ています。
    * データ構造を再帰し、見つけたすべてのLeafに関数を適用します。
    * ファンクタの法則は、直観的には、ブランチとリーフのノードのパターンが同じで、同じ構造を保持することを必要とします。
    */
//  import cats.Functor
//  import cats.syntax.functor._ // for map
//
//  implicit val treeFunctor: Functor[Tree] =
//    new Functor[Tree] {
//      def map[A, B](tree: Tree[A])(func: A => B): Tree[B] =
//        tree match {
//          case Branch(left, right) =>
//            Branch(map(left)(func), map(right)(func))
//          case Leaf(value) =>
//            Leaf(func(value))
//        }
//    }

//  Branch(Leaf(10), Leaf(20)).map(_ * 2)

  /**
    * おっと! これはセクション1.6.1で説明したのと同じ不変性の問題に陥ります。
    * コンパイラは Tree の場合はファンクタのインスタンスを見つけることができますが、Branch や Leaf の場合は見つけることができません。
    * それを補うために、いくつかのスマートなコンストラクタを追加してみましょう。
    */
//  object Tree {
//    def branch[A](left: Tree[A], right: Tree[A]): Tree[A] =
//      Branch(left, right)
//
//    def leaf[A](value: A): Tree[A] =
//      Leaf(value)
//  }

  /**
    * これでファンクタを適切に使うことができます。
    */
//  println(Tree.leaf(100).map(_ * 2))
////  // res9: Tree[Int] = Leaf(200)
////
//  println(Tree.branch(Tree.leaf(10), Tree.leaf(20)).map(_ * 2))
  // res10: Tree[Int] = Branch(Leaf(20), Leaf(40))

  // ## Contravariant(共変) and Invariant(不変) Functors {#contravariant-invariant}
  /**
    * これまで見てきたように、Functor の map メソッドは、変換を連鎖に「追加」するものと考えることができます。
    * ここで、他の2つの型クラスを見てみましょう。
    * 1つはチェーンへの前置操作を表すもので、もう1つは双方向チェーンの構築を表すものです。
    * これらはそれぞれ contravariant ファンクタ、invariant ファンクタと呼ばれています。
    */
  /**
    * このセクションはオプションです
    *
    * この本の中で最も重要なパターンであり、次の章の焦点でもあるモナドを理解するために、ContravariantInvariantと不変ファンクタについて知る必要はありません。
    * しかし、第6章の半群論と応用論の議論では、逆行列と不変は便利です。
    * もしあなたが今すぐモナドの話に進みたいのであれば、第4章まで読み飛ばしてください。
    * 第6章を読む前にここに戻ってきてください。
    */
  // 3.5.5 Contravariant Functors and the contramap Method

  /**
    * 最初の型クラスである contravariant functor は、contramap と呼ばれる操作を提供します。
    * 一般的な型シグネチャを図5に示します．
    */
  // == 図5 ==

  /**
    * contramapメソッドは変換を表すデータ型に対してのみ意味を持ちます。
    * 例えば Option[B] の値を関数 A => B で逆算する方法がないので、Option の contramap を定義することはできません。
    */
//  trait Printable[A] {
//    def format(value: A): String
//  }

  /**
    * Printable[A] は，A から String への変換を表します。
    * そのcontramapメソッドは，B => A型の関数funcを受け取り，新しいPrintable[B]を作成します．
    */
//  trait Printable[A] {
//    def format(value: A): String
//
//    def contramap[B](func: B => A): Printable[B] =
//      ???
//  }
//
//  def format[A](value: A)(implicit p: Printable[A]): String =
//    p.format(value)

  // 3.5.5.1 Exercise: Showing off with Contramap
  /**
    * 上記のPrintable用のcontramapメソッドを実装します。
    * 以下のコードテンプレートから始めて、???を機能するものに置き換えてください。
    */
//  trait Printable[A] {
//    def format(value: A): String
//
//    def contramap[B](func: B => A): Printable[B] =
//      new Printable[B] {
//        def format(value: B): String =
//          self.format(func(value))
//      }
//  }

  /**
    * 行き詰ったら型について考えてみましょう。
    * B型のvalueをStringにする必要があります。
    * どのような関数やメソッドが用意されていて、それらをどのような順番で組み合わせる必要があるのでしょうか？
    */
  /**
    * ここでは、実際の実装を紹介します。funcを呼び出してBをAに変換し、オリジナルのPrintableを使ってAを文字列に変換します。
    * ちょっとした手の巧妙さで、外側と内側のPrintableを区別するために自己のエイリアスを使用していま
    *
    */
//  trait Printable[A] { self =>
//
//    def format(value: A): String
//
//    def contramap[B](func: B => A): Printable[B] =
//      new Printable[B] {
//        def format(value: B): String =
//          self.format(func(value))
//      }
//  }
//
//  def format[A](value: A)(implicit p: Printable[A]): String =
//    p.format(value)

  /**
    * テストのために、文字列とブール値のPrintableのインスタンスをいくつか定義してみましょう。
    */
//  implicit val stringPrintable: Printable[String] =
//    new Printable[String] {
//      def format(value: String): String =
//        s"'${value}'"
//    }
//
//  implicit val booleanPrintable: Printable[Boolean] =
//    new Printable[Boolean] {
//      def format(value: Boolean): String =
//        if (value) "yes" else "no"
//    }

//  format("hello")
//  // res2: String = "'hello'"
//  format(true)
//  // res3: String = "yes"

  /**
    * ここで、次のBoxケースクラスのPrintableのインスタンスを定義します。
    * これを1.2.3節で説明したように暗黙のdefとして記述する必要があります。
    */
//  final case class Box[A](value: A)

  /**
    * 完全な定義を一から書き出すのではなく（新しいPrintable[Box]など）、
    * contramapを使って既存のインスタンスからインスタンスを作成します。
    * インスタンスは以下のように動作します。
    */
//  format(Box("hello world"))
//  // res4: String = "'hello world'"
//  format(Box(true))
//  // res5: String = "yes"

  /**
    * Box内の型のPrintableがない場合、Formatの呼び出しはコンパイルに失敗するはずです。
    */
//  format(Box(123))
//   error: could not find implicit value for parameter p: repl.Session.App1.Printable[repl.Session.App1.Box[Int]]
//   def encode(value: B): String =

  /**
    * インスタンスをすべてのタイプのBoxに共通化するために、Box内のタイプのPrintableをベースにしています。
    * 完全な定義を手書きで書き出すこともできます。
    */
//  implicit def boxPrintable[A](
//      implicit p: Printable[A]
//  ): Printable[Box[A]] =
//    new Printable[Box[A]] {
//      def format(box: Box[A]): String =
//        p.format(box.value)
//    }
//
//  println(format(Box("hello world")))
//  println(format(Box(true)))
//
//
//  implicit def boxPrintable[A](implicit p: Printable[A]): Printable[Box[A]] =
//    p.contramap[Box[A]](_.value)

  // 3.5.6 Invariant functors and the imap method

  /**
    * 不変関数は、非公式には map と contramap の組み合わせと同等の imap と呼ばれるメソッドを実装しています。
    * mapが連鎖に関数を追加することで新しい型クラスのインスタンスを生成し、
    * contramapが連鎖に操作を前置することでインスタンスを生成するとすると、imapは一対の双方向変換によってインスタンスを生成します。
    * 最も直感的な例としては、Play JSONのFormatやscodecのCodecのように、
    * エンコードとデコードを何らかのデータ型として表現する型クラスがあります。
    * Printable を拡張して、文字列へのエンコードとデコードをサポートするようにすることで、独自の Codec を構築することができます。
    */
//  trait Codec[A] {
//    def encode(value: A): String
//    def decode(value: String): A
//    def imap[B](dec: A => B, enc: B => A): Codec[B] = ???
//  }
//
//  def encode[A](value: A)(implicit c: Codec[A]): String =
//    c.encode(value)
//
//  def decode[A](value: String)(implicit c: Codec[A]): A =
//    c.decode(value)

  /**
    * imapの型図を図6に示します。Codec[A]と関数A => BとB => Aのペアがあれば、imapメソッドはCodec[B]を作成します。
    */
  // == 図6 ==

  /**
    * 使用例として、基本的な Codec[String] があり、そのエンコードとデコードの両方のメソッドは、単に渡された値を返すだけです。
    */
//  implicit val stringCodec: Codec[String] =
//    new Codec[String] {
//      def encode(value: String): String = value
//      def decode(value: String): String = value
//    }

  /**
    * imapを使ってstringCodecから構築することで、他のタイプのために多くの便利なCodecを構築することができます。
    */
//  implicit val intCodec: Codec[Int] =
//    stringCodec.imap(_.toInt, _.toString)
//
//  implicit val booleanCodec: Codec[Boolean] =
//    stringCodec.imap(_.toBoolean, _.toString)

  /**
    * 失敗への対応
    *
    * Codec 型クラスの decode メソッドは失敗を考慮していないことに注意してください。
    * もし、より洗練された関係をモデル化したいのであれば、ファンクタを超えてlensesやopticsに目を向けることができます。
    * opticsについてはこの本の範囲を超えています。しかし、Julien TruffautのライブラリMonocleは、
    * さらなる調査のための素晴らしい見解を与えてくれています。
    */
  // 3.5.6.1 Transformative Thinking with imap

  /**
    * 上記Codec用のimapメソッドを実装します。
    */
  trait Codec[A] {
    self =>
    def encode(value: A): String

    def decode(value: String): A

    def imap[B](dec: A => B, enc: B => A): Codec[B] = {
      new Codec[B] {
        def encode(value: B): String =
          self.encode(enc(value))

        def decode(value: String): B =
          dec(self.decode(value))
      }
    }
  }

  /**
    * これはstringCodecのimapメソッドを使って実装することができます。
    */
//   implicit val doubleCodec: Codec[Double] =
//     stringCodec.imap[Double](_.toDouble, _.toString)

  /**
    * 最後に、以下のBoxタイプのCodecを実装します。
    */
  /**
    * Box[A]用の汎用Codecが必要です。Codec[A]上でimapを呼び出すことで作成します。
    */
//  final case class Box[A](value: A)
//  implicit def boxCodec[A](implicit c: Codec[A]): Codec[Box[A]] =
//    c.imap[Box[A]](Box(_), _.value)

//  Codec.encode(123.4)
//  // res11: String = "123.4"
//  decode[Double]("123.4")
//  // res12: Double = 123.4
//
//  encode(Box(123.4))
//  // res13: String = "123.4"
//  decode[Box[Double]]("123.4")
//  // res14: Box[Double] = Box(123.4)

  /**
    * 名前の意味は？
    * contravariance"、"invariance"、"covariance "という用語と、これらの異なる種類のファンクタとの間にはどのような関係があるのでしょうか？
    * 1.6.1項を思い出していただければ、分散はサブタイピングに影響を与えます。
    * サブタイプ化は変換とみなすことができます。B が A のサブタイプである場合、常に B を A に変換することができます。
    * 同様に、関数B => Aが存在すれば、BはAのサブタイプであると言えます。
    * Fがcovariant functorであれば，F[B]とcovariant B => Aがあれば，常にF[A]に変換できます。
    * contravariantファンクタは逆のケースを捉えます。
    * Fがcontravariantファンクタならば，F[A]とB => Aの変換があればいつでも、F[B]に変換できます。
    * 最後に、不変ファンクタは、関数A => Bを介してF[A]からF[B]に変換でき、逆に関数B => Aを介してF[B]に変換できる場合を捉えます。
    */
  // 3.6 Contravariant and Invariant in Cats

  /**
    * ここでは、cats.Contravariant(反変)とcats.Invariant型(不変)クラスによって提供される、
    * Catsにおける contravariantとinvariantファンクタの実装を見てみましょう。
    * ここではコードを簡略化したものを示します。
    */
//  trait Contravariant[F[_]] {
//    def contramap[A, B](fa: F[A])(f: B => A): F[B]
//  }
//
//  trait Invariant[F[_]] {
//    def imap[A, B](fa: F[A])(f: A => B)(g: B => A): F[B]
//  }

  // 3.6.1 Contravariant in Cats

  /**
    * Contravariant.applyメソッドを使ってContravariant(反変)のインスタンスを呼び出すことができます。
    * Catsは、Eq、Show、Function1などのパラメータを使用するデータ型のインスタンスを提供しています。以下に例を示します。
    */
  import cats.Contravariant
  import cats.Show
  import cats.instances.string._

  val showString = Show[String]

  val showSymbol = {
    Contravariant[Show].contramap(showString)((sym: Symbol) => s"'${sym.name}")
  }

  print(showSymbol.show(Symbol("dave")))
//   res1: String = "'dave"

  /**
    * より便利なのは、contramap 拡張メソッドを提供する cats.syntax.contravariant を使うことです。
    */
  import cats.syntax.contravariant._ // for contramap

  showString
    .contramap[Symbol](sym => s"'${sym.name}")
    .show(Symbol("dave"))
  // res2: String = "'dave"

  // 3.6.2 Invariant(不変) in Cats
  /**
    * 他の型の中で、CatsはMonoidのためのInvariantのインスタンスを提供しています。
    * これは、3.5.6節で紹介したCodecの例とは少し異なります。Monoidはこんな感じです。
    */
//  trait Monoid[A] {
//    def empty: A
//    def combine(x: A, y: A): A
//  }

  /**
    * ScalaのSymbol型用のMonoidを作りたいと想像してみてください。CatsはSymbol用のMonoidを提供していませんが、似たような型のMonoidを提供しています。
    * Stringです。
    * 空のStringに依存する空のメソッドと、次のように動作するcombineメソッドを使って、新しいセミグループを書くことができます。
    */
  /**
    * 1. 2つのシンボルをパラメータとして受け取ります。
    * 2. Symbolを文字列に変換します
    * 3. Monoid[String]を使って文字列を結合します。
    * 4. 結果をSymbolに変換します。
    *
    * String => Symbol と Symbol => String 型の関数をパラメータに渡すことで、 imap を使って combine を実装することができます。
    * 以下は、cats.syntax.invariantで提供されているimap拡張メソッドを使って書き出したコードです。
    */
  import cats.Monoid
  import cats.instances.string._ // for Monoid
  import cats.syntax.invariant._ // for imap
  import cats.syntax.semigroup._ // for |+|

  implicit val symbolMonoid: Monoid[Symbol] =
    Monoid[String].imap(Symbol.apply)(_.name)

  Monoid[Symbol].empty
  // res3: Symbol = '

  Symbol("a") |+| Symbol("few") |+| Symbol("words")
  // res4: Symbol = 'afewwords

  // 3.7 Aside: Partial Unification

  /**
    * 3.2節では、Function1のファンクタのインスタンスを見ました。
    */
  import cats.Functor
  import cats.instances.function._ // for Functor
  import cats.syntax.functor._ // for map

  val func1 = (x: Int) => x.toDouble
  val func2 = (y: Double) => y * 2

  val func3 = func1.map(func2)
  // func3: Int => Double = scala.Function1$$Lambda$6493/63932183@157213ca

  /**
    * Function1には2つの型のパラメータ（関数引数と結果型）があります。
    */
  trait Function1[-A, +B] {
    def apply(arg: A): B
  }

  /**
    * ただし、Functorは1つのパラメータを持つコンストラクタの型を受け付けます。
    */
//  trait Functor[F[_]] {
//    def map[A, B](fa: F[A])(func: A => B): F[B]
//  }

  /**
    * コンパイラは、Functorに渡す正しい種類の型コンストラクタを作成するために、
    * Function1の2つのパラメータのうちの1つを修正しなければなりません。
    * これには2つのオプションがあります。
    */
//  type F[A] = Int => A
//  type F[A] = A => Double

  /**
    * 前者が正しい選択であることはわかっています。しかし、コンパイラはコードの意味を理解していません。
    * その代わりにコンパイラは単純なルールに依存しており、「部分統一」と呼ばれるものを実装しています。
    * Scalaコンパイラの部分統一は、型のパラメータを左から右に固定することで動作します。
    * 上の例では、Int => DoubleのIntを固定し、Int => ?の型の関数のFunctorを探しています。
    */
//  type F[A] = Int => A
//
//  val functor = Functor[F]

  /**
    * この左から右への消去は、Function1やEitherのような型のFunctorsなど、さまざまな一般的なシナリオに対応しています。
    */
  val either: Either[String, Int] = Right(123)
//  // either: Either[String, Int] = Right(123)

  either.map(_ + 1)
  // res0: Either[String, Int] = Right(124)

  // 3.7.1 Limitations of Partial Unification(部分的な統合の制限)
  /**
    * 左から右への消去が正しくないことがあります。
    * 一例として、Scalactic の Or 型がありますが、れは、従来は左バイアスのいずれかと同等です。(意味がちょとわからなかった...)
    */
//  type PossibleResult = ActualResult Or Error

  /**
    * 別の例として、Function1のContravariant(反変)ファンクタがあります。
    * Function1の共分散ファンクタがandThenスタイルの左から右への関数合成を実装しているのに対し、
    * Contravariantファンクタは合成スタイルの右から左への関数合成を実装しています。言い換えれば、以下の式はすべて等価です。
    */
  val func3a: Int => Double =
    a => func2(func1(a))

  val func3b: Int => Double =
    func2.compose(func1)

  // Hypothetical example. This won't actually compile:
//  val func3c: Int => Double =
//    func2.contramap(func1)

  /**
    * しかし、これを実際に試してみると、コードはコンパイルできません。
    */
//  import cats.syntax.contravariant._ // for contramap
//
//  val func3c = func2.contramap(func1)
//  // error: value contramap is not a member of Double => Double
//  // val func3c = func2.contramap(func1)
//  //              ^^^^^^^^^^^^^^^

  /**
    * ここでの問題は、Function1のContravariantが戻り値の型を固定し、
    * パラメータの型を変化させたままにしているため、以下の図7に示すように、コンパイラが右から左へと型パラメータを排除する必要があることです。
    */

//  type F[A] = A => Double

  // == 図7 ==

  /**
    * コンパイラは単に左から右に偏っているために失敗します。
    * これを証明するには、Function1のパラメータを反転させる型のエイリアスを作成します。
    */

//  type <=[B, A] = A => B
//
//  type F[A] = Double <= A

  /**
    * func2を<=のインスタンスとして再タイプすると、必要な消去順序をリセットし、
    * 必要に応じてcontramapを呼び出すことができます。
    */

//  val func2b: Double <= Double = func2
//
//  val func3c = func2b.contramap(func1)
  // func3c: Int => Double = scala.Function1$$Lambda$6493/63932183@6d3804ec

  /**
    * func2 と func2b の違いは純粋に構文的なもので、どちらも同じ値を参照し、型のエイリアスは完全に互換性があります。
    * しかし、信じられないことに、この単純な言い換えだけで、問題を解決するために必要なヒントをコンパイラに与えるのに十分です。
    * このような右から左への消去を行わなければならないことは稀です。
    * ほとんどのマルチパラメータ型コンストラクタは右に偏るように設計されているため、コンパイラがサポートしている左から右への消去が必要になります。
    * しかし、上記のような奇妙なシナリオに遭遇した場合に備えて、この消去順序の奇妙さについて知っておくと便利です。
    */

  // 3.8 Summary
  /**
  * ファンクタはシーケンスの動作を表します。この章では、3種類のファンクタを取り上げました。
  *
  * - 正規共分散関数は、マップ・メソッドを使用して、あるコンテキスト内の値に関数を適用する機能を表します。
  * マップを連続して呼び出すと、これらの関数が順番に適用され、それぞれが前任者の結果をパラメータとして受け取ります。
  *
  * - contramapメソッドを持つcontravariant functorsは、関数のようなコンテキストに関数を「前置」する機能を表しています。
  * contramap を連続して呼び出すと、これらの関数はマップとは逆の順番で順番に並べられます。
  *
  * - imap法を用いた不変関数は双方向変換を表します。
  *
  * 正規関数はこれらの型クラスの中で最も一般的なものですが、それでも単独で使用することはほとんどありません。
  * 関数は、私たちが常に使用しているいくつかの興味深い抽象化の基礎的な構成要素を形成しています。
  * 次の章では、これらの抽象化のうちの2つの抽象化、すなわち、モナドとアプリカティブ・ファンクタについて見ていきます。
  *
  * コレクションのための関数は、各要素を他の要素から独立して変換するため、非常に重要である。
  * これにより、大規模なコレクションの変換を並列化したり分散したりすることが可能になる。
  * このアプローチについては、この本の後半でMap-reduceのケーススタディで詳しく調査する予定である。
  *
  * Contravariant型クラスとInvariant型クラスは、あまり広くは使われていませんが、変換を表すデータ型を構築するのにはまだ有用です。
  * これらを再検討して、第6章でSemigroup型クラスについて述べることにします。
  */

}
