package cats.Chapter2

import cats.Semigroup

object MonoidsAndSemigroups {

  /**
  Monoids and Semigroups
  この節では、最初の型クラスであるモノイドとSemigroupを探索します。
  これらのクラスでは、値を追加したり組み合わせたりすることができます。
  Ints、文字列、リスト、オプション、その他多くのインスタンスがあります。
  どのような共通の原理を引き出すことができるのか、いくつかの簡単な型と操作を見てみましょう。

  Integer addition
  Intsの加算は、2つのIntsを加算すると常に別のIntが生成されることを意味する閉じた2値演算です。

  2 + 1
  // res0: Int = 3

  また、任意の Int a に対して a + 0 == 0 + a == a という性質を持つ identity(恒等 こうとう: xがどんな値でも恒に等しい) 要素 0 もあります。
  2 + 0
  // res1: Int = 2

  0 + 2
  // res2: Int = 2

  また、足し算には他の特性もあります。
  例えば、要素を追加する順番は関係ありません。これは連想性と呼ばれる性質です。

  (1 + 2) + 3
  // res3: Int = 6

  1 + (2 + 3)
  // res4: Int = 6

  Integer multiplication
  0 の代わりに 1 を等号として使用することを条件に、足し算の同じ特性は乗算にも適用されます。

  1 * 3
  // res5: Int = 3

  3 * 1
  // res6: Int = 3

  掛け算は足し算と同じように連想的なものです。
  (1 * 2) * 3
  // res7: Int = 6

  1 * (2 * 3)
  // res8: Int = 6

  String and sequence concatenation
  バイナリ演算子として文字列の連結を使用して、文字列を追加することもできます。

  "One" ++ "two"
  // res9: String = "Onetwo"

  そして、空Stringをidentityとします

  "" ++ "Hello"
  // res10: String = "Hello"

  "Hello" ++ ""
  // res11: String = "Hello"

  繰り返しになりますが、連結は連想的なものです。
  ("One" ++ "Two") ++ "Three"
  // res12: String = "OneTwoThree"

  "One" ++ ("Two" ++ "Three")
  // res13: String = "OneTwoThree"

  シーケンスとの並列性を示唆するために，通常の + の代わりに ++ を使用したことに注意してください。
  他のタイプのシーケンスについても同様のことができます．
    */
  /**
  2.1 Definition of a Monoid
  それぞれの上にある「加算」のシナリオには、連想的な二進法の加算と同一性の要素がありました。
  これがモノイドであることを知っても不思議ではありません。形式的には、A型のモノイドは次のようになります。
    - 型(A, A)と組み合わせた操作 => A
    - 型 A の空の要素

  この定義はScalaのコードにうまく変換されます。以下は Cats の定義を簡略化したものです。*/
//  trait Monoid[A] {
//    def combine(x: A, y: A): A
//    def empty: A
//  }

  /**
    * モノイドは、combineとempty演算を提供することに加えて、いくつかの法則に正式に従わなければなりません。
    * Aのすべての値x, y, zについて、combineは連想的でなければならず、emptyはidentity要素でなければなりません。
    * */
  def associativeLaw[A](x: A, y: A, z: A)(implicit m: Monoid[A]): Boolean = {
    m.combine(x, m.combine(y, z)) ==
      m.combine(m.combine(x, y), z)
  }

  def identityLaw[A](x: A)(implicit m: Monoid[A]): Boolean = {
    (m.combine(x, m.empty) == x) &&
    (m.combine(m.empty, x) == x)
  }

  /**
    * 例えば、整数の減算は、減算が連想的ではないので、モノイドではありません。
    */
//  (1 - 2) - 3
//  // res14: Int = -4
//
//  1 - (2 - 3)
//  // res15: Int = 2
  /**
    * 実際には、私たちは、私たち自身のMonoidインスタンスを書いているときだけ法則について考える必要があります。
  非法則的なインスタンスは、危険です。なぜなら、Monoid以外のCatsの動きが、予測できない結果を出す可能性があるからです。
  ほとんどの場合、私たちはCatsによって提供されたインスタンスに頼ることができ、ライブラリの著者は彼らが何をしているか知っていると仮定します。

  2.2 Definition of a Semigroup
  Semigroupはモノイドの結合部分だけで、空の部分がありません。

  多くのSemigroupもモノイドですが、空の要素を定義できないデータ型もあります。
  例えば、シーケンスの連結と整数の加算がモノイドであることを見てきましたが、
  空でないシーケンスと正の整数に限定すると、空の要素を定義できなくなります。
  しかし、非空でない配列と正の整数に限定すると、もはや感覚的な空要素を定義することはできません。
  Catsには、Semigroupの実装はありますが、Monoidの実装がないNonEmptyListデータ型があります。

  Cats' Monoidのより正確な（まだ簡略化されていますが）定義は次のとおりです。*/
//  trait Semigroup[A] {
//    def combine(x: A, y: A): A
//  }
//
//  trait Monoid[A] extends Semigroup[A] {
//    def empty: A
//  }

  /**
    * 型クラスの話をしていると、このような継承をよく目にするでしょう。
    * これはモジュール性を提供し、動作を再利用することを可能にします。
    * A型にMonoidを定義すると、Semigroupを容易にで手に入れることができます。
    * 同様に、あるメソッドがSemigroup[B]型のパラメータを必要とする場合、代わりにMonoid[B]を渡すことができます。
    *
    * 2.3 Exercise: The Truth About Monoids
    * モノイドの例をいくつか見てきましたが、他にもたくさんあります。
    * ブール型を考えてみましょう。この型に対して、いくつのモノイドを定義できますか？
    * それぞれのモノイドについて、結合演算と空演算を定義し、モノイドの法則が保持されていることを納得させてください。
    * 以下の定義を出発点として使用してください。*/
  trait Semigroup[A] {
    def combine(x: A, y: A): A
  }

  trait Monoid[A] extends Semigroup[A] {
    def empty: A
  }

  object Monoid {
    def apply[A](implicit monoid: Monoid[A]) =
      monoid
  }

  /**
    * 2.3 Exercise: The Truth About Monoids
    * 問題の意図わからず答えみました。
    * ブーリアンには4つのモノイドがあります。まず、演算子&&と恒等式真を持つandがあります。
    */
  implicit val booleanAndMonoid: Monoid[Boolean] =
    new Monoid[Boolean] {
      def combine(a: Boolean, b: Boolean) = a && b
      def empty = true
    }

  /**
    * 第二に、演算子｜｜を持つorと、IDのfalseを持つ
    */
  implicit val booleanOrMonoid: Monoid[Boolean] =
    new Monoid[Boolean] {
      def combine(a: Boolean, b: Boolean) = a || b
      def empty = false
    }

  /**
    * 第三に、私たちは排他的またはアイデンティティのfalseを持っている
    */
  implicit val booleanEitherMonoid: Monoid[Boolean] =
    new Monoid[Boolean] {
      def combine(a: Boolean, b: Boolean) =
        (a && !b) || (!a && b)
      def empty = false
    }

  /**
    *　最後に、exclusive nor（exclusive orの否定）とidentity trueを持つ
    */
  implicit val booleanXnorMonoid: Monoid[Boolean] =
    new Monoid[Boolean] {
      def combine(a: Boolean, b: Boolean) =
        (!a || b) && (a || !b)
      def empty = true
    }

  /**
    * それぞれのケースで恒等法が成り立つことを示すのは簡単です。
    * 同様に、結合操作の連想性は、以下のケースを列挙することで示すことができます。
    */
  /**
    * 2.4 Exercise: All Set for Monoids
    * 集合にはどんなモノイドやSemigroupがあるか？
    */
  implicit def setUnionMonoid[A]: Monoid[Set[A]] =
    new Monoid[Set[A]] {
      def combine(a: Set[A], b: Set[A]) = a union b
      def empty = Set.empty[A]
    }

  /**
    * setUnionMonoidを値ではなくメソッドとして定義し、タイプパラメータAを受け付ける必要があります。
    * タイプパラメータを使用することで、同じ定義を使用して、あらゆるタイプのデータのセットに対してモノイドを召喚することができます。
    * */
//  val intSetMonoid = Monoid[Set[Int]]
//  val strSetMonoid = Monoid[Set[String]]
  def main(args: Array[String]): Unit = {
//    println(intSetMonoid.combine(Set(1, 2), Set(2, 3))) // Set(1, 2, 3)
//    println(strSetMonoid.combine(Set("A", "B"), Set("B", "C"))) // Set(A, B, C)
  }

  /**
    * 集合の交点はSemigroupを形成するが、同一の要素を持たないのでモノイドを形成しない
    */
  implicit def setIntersectionSemigroup[A]: Semigroup[Set[A]] =
    new Semigroup[Set[A]] {
      def combine(a: Set[A], b: Set[A]) = a intersect b
    }

  /**
    * 集合補集合と集合差は連想的ではないので、モノイドにもSemigroupにも考えられません。
    * しかし、対称的な差（和から交を引いたもの）は、空集合であるモノイド
    */
  implicit def symDiffMonoid[A]: Monoid[Set[A]] =
    new Monoid[Set[A]] {
      def combine(a: Set[A], b: Set[A]): Set[A] =
        (a diff b) union (b diff a)
      def empty: Set[A] = Set.empty
    }

  /**
    * 2.5 Monoids in Cats
    *
    * モノイドとは何かを見てきましたが、次にCatsでの実装を見てみましょう。
    * ここでは、実装の3つの主要な側面、すなわち、型クラス、インスタンス、そしてインターフェースを見ていきます。
    *
    * 2.5.1 The Monoid Type Class
    *
    * モノイド型のクラスは cats.kernel.Monoid で、これは cats.Monoid と呼ばれています。
    * Monoid は cats.kernel.Semigroup を継承しており、これは cats.Semigroup と呼ばれています。
    * Catsを使うときは、通常、catsパッケージから型クラスをインポートします。
    *
    * Cats Kernel?
    *
    * Cats Kernel は Cats のサブプロジェクトで、完全な Cats ツールボックスを必要としないライブラリのために、小さな型クラスのセットを提供しています。
    * これらの中核となる型クラスは、技術的には cats.kernel パッケージで定義されていますが、すべて cats パッケージにエイリアスされているため、その区別を意識する必要はほとんどありません。
    *
    * 本書で扱うCats Kernelの型クラスは、Eq、半群、Monoidです。
    * 本書で扱うその他の型クラスはすべて、Catsプロジェクトの一部であり、catsパッケージで直接定義されています。
    *
    * 2.5.2 Monoid Instances
    *
    * Monoidは、ユーザー・インターフェースの標準的なCatsパターンに従っています。
    * コンパニオン・オブジェクトは、特定の型に対する型クラス・インスタンスを返すapplyメソッドを持っています。
    * 例えば、Stringのmonoidインスタンスが欲しい場合、正しい暗黙の了解があれば、次のように書くことができます。
    */
  import cats.instances.string._ // for Monoid

//  Monoid[String].combine("Hi ", "there")
  // res0: String = "Hi there"
//  Monoid[String].empty
  // res1: String = ""

  /**
    * こちらも同様です。
    */
//  Monoid.apply[String].combine("Hi ", "there")
//  // res2: String = "Hi there"
//  Monoid.apply[String].empty
  // res3: String = ""

  /**
    * ご存知のように、MonoidはSemigroupを拡張しています。
    * もし、emptyが不要であれば同じように記載できます。
    */
//  import cats.Semigroup

  Semigroup[String].combine("Hi ", "there")
  // res4: String = "Hi there"

  /**
    * Monoidの型クラスのインスタンスは、第1章で説明した標準的な方法でcats.instancesの下に整理されています。
    * 例えば、Intのインスタンスを取り込みたい場合は、cats.instances.intからインポートします。
    */

//  Monoid[Int].combine(32, 10)
  // res5: Int = 42

  /**
    * 同様に、cats.instances.intとcats.instances.optionのインスタンスを使って、Monoid[Option[Int]]を組み立てることができます。
    */

  val a = Option(22)
  // a: Option[Int] = Some(22)
  val b = Option(20)
  // b: Option[Int] = Some(20)

//  Monoid[Option[Int]].combine(a, b)
  // res6: Option[Int] = Some(42)

  /**
    * インポートのより詳細なリストについては、第1章を参照してください。
    * いつものように、個々のインスタンスをインポートする正当な理由がない限り、すべてをインポートすればよいのです。
    */

  /**
    * 2.5.3 Monoid Syntax
    *
    * Catsではcombineメソッドの構文を｜+｜演算子の形で提供しています。
    * combineは技術的にはSemigroupから来ているので、cats.syntax.semigroupからインポートしてシンタックスにアクセスします。
    */

//  val stringResult = "Hi " |+| "there" |+| Monoid[String].empty
  // stringResult: String = "Hi there"

//  val intResult = 1 |+| 2 |+| Monoid[Int].empty
  // intResult: Int = 3

  /**
    * 2.5.4 Exercise: Adding All The Things
    *
    * 最先端の SuperAdder v3.5a-32 は、数字の足し算をするための世界初の選択肢です。
    * このプログラムの主な関数には署名があります def add(items: List[Int]): Int. 悲劇的な事故で、このコードは削除されてしまいました。
    * このメソッドを書き換えて、窮地を脱しましょう
    *
    * 0と+演算子を使って、foldLeftとして加算を書くことができます。
    */
  def add(items: List[Int]): Int =
    items.foldLeft(0)(_ + _)

  /**
    * 別の方法として、Monoids を使用して折り畳みを記述することもできますが、
    * これについてはまだ説得力のある使用例がありません。
    */

//  def add(items: List[Int]): Int =
//    items.foldLeft(Monoid[Int].empty)(_ |+| _)

  /**
    * お疲れ様です。SuperAdder のシェアは拡大を続けていますが、今では追加機能の需要があります。
    * List[Option[Int]]を追加したいという要望があります。これが可能になるように add を変更してください。
    * SuperAdder のコードベースは最高品質なので、コードの重複がないようにしてくださいね。
    *
    * ここで、モノイドのユースケースがあります。IntsとOption[Int]のインスタンスを追加する単一のメソッドが必要です。
    * これは、暗黙のMonoidをパラメータとして受け取るジェネリック・メソッドとして書くことができます。
    */

//  def add[A](items: List[A])(implicit monoid: Monoid[A]): A =
//    items.foldLeft(monoid.empty)(_ |+| _)

  /**
    * オプションでScalaのコンテキスト・バインド・シンタックスを使うと、同じコードをより短く書くことができます。
    */
//  def add[A: Monoid](items: List[A]): A =
//    items.foldLeft(Monoid[A].empty)(_ |+| _)

  /**
    * このコードを使って、要求に応じてInt型やOption[Int]の値を追加することができます。
    */

  add(List(1, 2, 3))
  // res10: Int = 6

//  add(List(Some(1), None, Some(2), None, Some(3)))
  // res11: Option[Int] = Some(6)

  /**
    * なお、Someの値だけで構成されたリストを追加しようとすると、コンパイルエラーが発生します。
    */
//  add(List(Some(1), Some(2), Some(3)))
  // error: could not find implicit value for evidence parameter of type cats.Monoid[Some[Int]]
  // add(List(Some(1), Some(2), Some(3)))
  // ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

  /**
    * これは、リストの推論型が List[Some[Int]] であるのに対し、Cats は Option[Int] の Monoid しか生成しないために起こります。こ
    * れを回避する方法は後ほど説明します。
    *
    * SuperAdderはPOS（Point-of-Sale、他のPOSではなく）市場に参入します。
    * 今、私たちはオーダを加えたい。
    */
  case class Order(totalCost: Double, quantity: Double)

  /**
    * このコードをすぐにリリースする必要があるので、追加のための修正はできません。
    */
  implicit val monoid: Monoid[Order] = new Monoid[Order] {
    def combine(o1: Order, o2: Order) =
      Order(
        o1.totalCost + o2.totalCost,
        o1.quantity + o2.quantity
      )

    def empty = Order(0, 0)
  }

  /**
    * 2.6 Applications of Monoids
    *
    * モノイドとは、足し算や組み合わせの概念を抽象化したものであることはわかりましたが、ではモノイドはどのようなところで役に立つのでしょうか。
    * ここでは、モノイドが重要な役割を果たすいくつかの大きなアイデアを紹介します。この本の後のケーススタディでは、これらをさらに詳しく説明しています。
    *
    * 2.6.1 Big Data
    *
    * SparkやHadoopのようなビッグデータアプリケーションでは、データ分析を多くのマシンに分散させ、耐障害性とスケーラビリティを実現しています。
    * つまり、それぞれのマシンがデータの一部の結果を返し、その結果を組み合わせて最終的な結果を得ることになります。
    * 大半の場合、これはモノイドと見なすことができます。
    *
    * Webサイトの総訪問者数を計算したい場合、データの各部分に対してIntを計算することになります。
    * Intのモノイドインスタンスは加算であることがわかっており、部分的な結果を結合するのに適しています。
    *
    * Webサイトのユニークビジターの数を調べたい場合は、データの各部分にSet[User]を構築することと同じです。
    * Setのモノイドインスタンスはset unionであることがわかっており、これは部分的な結果を結合する正しい方法です。
    *
    * サーバーログから99%と95%の応答時間を計算したい場合は、モノイドが存在するQTreeと呼ばれるデータ構造を使用することができます。
    *
    * ご理解いただけましたでしょうか。
    * 大規模なデータセットに対して行いたいと思うほとんどすべての分析はモノイドであるため、このアイデアに基づいて表現力豊かで強力な分析システムを構築することができます。
    * これはまさに、TwitterのAlgebirdやSummingbirdプロジェクトが行っていることです。このアイデアについては、map-reduceのケーススタディでさらに詳しく説明します。
    *
    * 2.6.2 Distributed Systems
    *
    * 分散システムでは、異なるマシンが異なるデータビューを持つことになります。
    * 例えば、あるマシンが、他のマシンが受信していないアップデートを受信したとします。
    * これらの異なる見解を調整して、今後更新がない場合は、すべてのマシンが同じデータを持つようにしたいと思います。
    * これをeventual consistencyと呼びます。
    *
    * 特定のクラスのデータ型がこの整合性をサポートします。
    * これらのデータ型はcommutative replicated data types (CRDT)と呼ばれます。
    * 重要な操作は、2つのデータインスタンスをマージして、両方のインスタンスのすべての情報をキャプチャする結果を得ることです。
    * この操作は、モノイドのインスタンスを持つことに依存しています。
    * このアイデアについては、CRDT(Conflict-free replicated data type)のケーススタディで詳しく説明します。
    *
    * 2.6.3 Monoids in the Small
    *
    * 上記の2つの例は、モノイドがシステムアーキテクチャ全体に影響を与えるケースです。
    * また、モノイドがあることで、小さなコードの断片を書くことが容易になるケースも多くあります。
    * この本のケーススタディでは、たくさんの例を見ることができます。
    *
    * 2.7 Summary
    *
    * この章では大きな節目を迎えました。関数型プログラミングの派手な名前を持つ最初の型クラスを取り上げました。
    *
    * Semigroupは、足し算や組み合わせの操作を表します。
    * モノイドはSemigroupを拡張して恒等式または「ゼロ」の要素を加えたものです。
    * Semigroupとモノイドを使用するには、次の3つのものをインポートします。
    * 型クラスそのもの、気になる型のインスタンス、そして｜+｜演算子を提供するSemigroup構文です。
    */

//  "Scala" |+| " with " |+| "Cats"
  // res0: String = "Scala with Cats"

  /**
    * 正しいインスタンスがスコープにあれば、必要なものを追加することができます。
    */

//  Option(1) |+| Option(2)
  // res1: Option[Int] = Some(3)

  val map1 = Map("a" -> 1, "b" -> 2)
  val map2 = Map("b" -> 3, "d" -> 4)

//  map1 |+| map2
  // res2: Map[String, Int] = Map("b" -> 5, "d" -> 4, "a" -> 1)

  val tuple1 = ("hello", 123)
  val tuple2 = ("world", 321)

//  tuple1 |+| tuple2
  // res3: (String, Int) = ("helloworld", 444)

  /**
    * また、Monoidのインスタンスを持っている任意の型で動作する汎用コードを書くこともできます。
    */

//  def addAll[A](values: List[A])
//               (implicit monoid: Monoid[A]): A =
//    values.foldRight(monoid.empty)(_ |+| _)
//
//  addAll(List(1, 2, 3))
//  // res4: Int = 6
//  addAll(List(None, Some(1), Some(2)))
  // res5: Option[Int] = Some(3)

  /**
  * モノイドはキャッツへの入り口として最適です。
  * 理解しやすく、使い方も簡単です。
  * しかし、Catsが可能にする抽象化という点では、氷山の一角に過ぎません。
  * 次の章では、ファンクタを見ていきます。
  * ファンクタは、愛されているマップメソッドの型クラスの擬人化です。
  * そこからが本当の楽しみなのです
  */
}
