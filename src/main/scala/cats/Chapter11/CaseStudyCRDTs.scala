package cats.Chapter11

object CaseStudyCRDTs extends App {

  /**
    * 11 Case Study: CRDTs
    *
    * このケーススタディでは、最終的に一貫性のあるデータを調整するために使用できるデータ構造のファミリーであるCommutative Replicated Data Types (CRDT)について調べます。
    * ※ Conflict-free replicated data typeしかでてこない...
    *
    * まず、最終的に一貫したシステムの有用性と難しさを説明し、次にモノイドとその拡張を使って発生する問題を解決する方法を示します。
    * 最後に、その解決策をScalaでモデル化します。
    *
    * ここでの目標は、特定のタイプのCRDTのScalaでの実装に焦点を当てることです。
    * すべてのCRDTを包括的に調査することを目指しているわけではありません。
    * CRDTは急速に発展している分野であり、より多くのことを学ぶために文献を読むことをお勧めします。
    *
    *
    * 11.1 Eventual Consistency
    *
    * システムの規模が1台のマシンを超えると、データをどのように管理するかという根本的な選択を迫られます。
    *
    * 1つのアプローチは、一貫性のあるシステムを構築することです。
    * つまり、すべてのマシンがデータに対して同じ見解を持つようにします。
    * 例えば、ユーザーがパスワードを変更した場合、そのパスワードのコピーを保存しているすべてのマシンがその変更を受け入れなければ、操作が正常に完了したとはみなされません。
    *
    * 一貫性のあるシステムは扱いやすいのですが、欠点もあります。
    * 1回の変更で多くのメッセージがマシン間で送信されるため、待ち時間が長くなる傾向があります。
    * また、障害が発生するとマシン間の通信が遮断され、ネットワーク・パーティションが形成されるため、稼働率が比較的低くなる傾向があります。
    * ネットワーク・パーティションが存在する場合、一貫性のあるシステムは、マシン間の不整合を防ぐためにさらなる更新を拒否することがあります。
    *
    * 別のアプローチとして、最終的に一貫性のあるシステムがあります。
    * これは、ある特定の時点で、マシンがデータの異なる見解を持つことが許されることを意味します。
    * しかし、すべてのマシンが通信可能で、今後の更新がなければ、最終的にはすべてのマシンが同じデータビューを持つことになります。
    *
    * 最終的に一貫性のあるシステムでは、マシン間のコミュニケーションが少なくて済むので、レイテンシーも低くなります。
    * パーティション化されたマシンは、ネットワークが固定されていても更新を受け入れ、その変更を調整することができるので、システムの稼働率も向上します。
    *
    * 問題は、このマシン間の調整をどのように行うかです。
    * CRDTはこの問題に対する一つのアプローチを提供します。
    *
    * 11.2 The GCounter
    *
    * ある特定のCRDTの実装を見てみましょう。
    * そして、一般的なパターンを見つけられるかどうか、プロパティを一般化することを試みます。
    *
    * ここで見ていくデータ構造はGCounterと呼ばれるものです。
    * これは分散型のインクリメントオンリーカウンタで、例えば、多くのウェブサーバでリクエストが処理されるウェブサイトへの訪問者数をカウントするのに使用できます。
    *
    * 11.2.1 Simple Counters
    *
    * 単純なカウンタが機能しない理由を説明するために、単純な訪問者数を保存する2つのサーバがあるとします。
    * それぞれのマシンには整数のカウンタが格納されており、図25に示すように、カウンタはすべてゼロから始まります。
    *
    * Figure 25: Simple counters: initial state
    *
    * ここで、あるウェブトラフィックを受信したとします。
    * ロードバランサーは、5つの受信リクエストをAとBに振り分け、Aは3人、Bは2人の訪問者に対応します。
    * これらのマシンでは、システムの状態についての見解が一致しておらず、整合性をとるためには調整が必要です。
    * 図26に示すように、単純なカウンタを使った照合方法の1つに、カウントを交換して追加する方法があります。
    *
    * Figure 26: Simple counters: first round of requests and reconciliation
    *
    * ここまでは良かったのですが、まもなく事態は悪化していきます。
    * Aが1人の訪問者に対応したとすると、合計で6人の訪問者がいたことになります。
    * マシンは足し算を使って再び状態を調整しようとし、図27のような答えを導き出します。
    *
    * Figure 27: Simple counters: second round of requests and (incorrect) reconciliation
    *
    * これは明らかに間違っています。
    * 問題は、単純なカウンターでは、マシン間の相互作用の履歴について十分な情報が得られないことです。
    * 幸いなことに、正しい答えを得るために完全な履歴を保存する必要はなく、その要約を得ることができます。
    * GCounterがこの問題をどのように解決するか見てみましょう。
    *
    * 11.2.2 GCounters
    *
    * GCounter の最初の巧妙なアイデアは、各マシンが、それが知っているすべてのマシン(自分自身を含む)に対して個別のカウンタを格納することです。
    * 前の例では、AとBの2台のマシンがありました。
    * この状況では、両方のマシンが図28に示すように、A用のカウンタとB用のカウンタを格納します。
    *
    * Figure 28: GCounter: initial state
    *
    * GCountersのルールは、あるマシンが自分のカウンタをインクリメントすることだけが許されるというものです。
    * Aが3人の訪問者にサービスを提供し、Bが2人の訪問者にサービスを提供する場合、カウンタは図29のようになります。
    *
    * Figure 29: GCounter: first round of web requests
    *
    * 2台のマシンがカウンターを照合する際には、各マシンに保存されている最大の値を取るというルールがあります。
    * この例では、最初のマージの結果は図30のようになります。
    *
    * Figure 30: GCounter: first reconciliation
    *
    * その後の受信Webリクエストはincrement-own-counterルールで処理され、その後のマージはtake-maximum-valueルールで処理され、図31に示すように各マシンに同じ正しい値が生成されます。
    *
    * Figure 31: GCounter: second reconciliation
    *
    * GCountersは、各マシンがインタラクションの完全な履歴を保存することなく、システム全体の状態を正確に把握することを可能にします。
    * マシンがウェブサイト全体の総トラフィックを計算したい場合、マシンごとのカウンタをすべて合計します。
    * その結果は、最近になって照合を行ったかどうかによって、正確かそれに近いものになります。
    * 最終的には、ネットワークの停止に関わらず、システムは常に一貫した状態に収束します。
    *
    * 11.2.3 Exercise: GCounter Implementation
    *
    * 以下のインターフェイスでGCounterを実装することができます。
    * ここでは、マシンIDをStringsとして表現しています。
    *
  final case class GCounter(counters: Map[String, Int]) {
    def increment(machine: String, amount: Int) = ???

  def merge(that: GCounter): GCounter = ???
  def total: Int =???
  }
  実装を完了させてください

    * 幸いなことに上記の説明は明快で、以下のように実装することができます。
    */
//  final case class GCounter(counters: Map[String, Int]) {
//    def increment(machine: String, amount: Int) = {
//      val v = amount + counters.getOrElse(machine, 0)
//      GCounter(counters + (machine -> v))
//    }
//
//    def merge(that: GCounter): GCounter =
//      GCounter(that.counters ++ this.counters.map {
//        case (k, v) =>
//          k -> (v max that.counters.getOrElse(k, 0))
//      })
//
//    def total: Int =
//      counters.values.sum
//  }

  /**
    * 11.3 Generalisation
    *
    * これで、分散した、最終的には一貫性のある、増分のみのカウンターができました。
    * これは有用な成果ですが、ここで終わらせたくはありません。
    * このセクションでは、GCounter の操作を抽象化して、自然数だけでなくより多くのデータ型で動作するようにします。
    *
    * GCounter は自然数に対して以下の操作を使用します。
    *
    * - 加算（incrementとtotalで）。
    * - 最大値（マージ時）。
    * - また、恒等式要素である 0（インクリメントおよびマージ時）も使用します。
    *
    * どこかにモノイドが入っていることは想像できると思いますが、頼りにしている特性をもっと詳しく見てみましょう。
    *
    * おさらいですが、第2章ではモノイドは2つの法則を満たさなければならないと説明しました。二項演算の+は連想的でなければならない。
    *
    * (a + b) + c == a + (b + c)
    *
    * であり、空の要素はアイデンティティでなければならない。
    *
    * 0 + a == a + 0 == a
    *
    * カウンタを初期化するためには、インクリメントのアイデンティティが必要です。
    * また、結合の特定の順序が正しい値を与えることを保証するために、連想性に依存しています。
    *
    * 合計すると、機械ごとのカウンタの合計をどのような任意の順序にしても、正しい値が得られるように、連想性と可換性に暗黙的に依存しています。
    * また、暗黙のうちに恒等式を仮定しているので、カウンタを保存していないマシンをスキップすることができます。
    *
    * マージの特性は、もう少し興味深いものです。マシンAがマシンBとマージするのと、マシンBがマシンAとマージするのとでは、同じ結果になることを保証するために、可換性に依存しています。
    * また、空のカウンターを初期化するために、ID要素が必要です。
    * 最後に、2台のマシンがマシンごとのカウンタに同じデータを保持している場合、データをマージしても正しくない結果にならないことを保証するために、idempotency(冪等性)と呼ばれる追加のプロパティが必要です。
    * idempotentな演算とは、複数回実行しても同じ結果が繰り返し返ってくるものである。
    * 形式的には、次のような関係が成り立つ場合、二項演算maxは冪等である。
    *
    * a max a = a
    *
    * よりコンパクトに書くと、次のようになります。
    *
    * Method  | Identity	| Commutative(可換)| Associative	| Idempotent |
    increment |	   Y	   |     N	      |     Y	      |     N      |
     merge	  |    Y	   |     Y	      |     Y	      |     Y      |
     total	  |    Y	   |     Y	      |     Y       |	    N      |

    * ここから、次のことがわかります。
    * - incrementはモノイドを必要とします。
    * - totalは、可換単項式を必要とする。
    * - mergeには、idempotent commutative monoidが必要で、bounded semilatticeとも呼ばれます。
    *
    * incrementとgetはどちらも同じ二項演算（加算）を使うので、両方とも同じ可換単項式を必要とするのが普通です。
    * この調査は、抽象概念の特性や法則について考えることの力を示しています。
    * さて，これらの性質を確認したので，GCounterで使われている自然数を，これらの性質を満たす演算を持つ任意のデータ型で置き換えることができます．
    * 簡単な例は、二項演算が和であり、同一の要素が空のセットであるセットです。
    * Int を Set[A] に置き換えるだけで、GSet 型を作成できます。
    *
    * 11.3.1 Implementation
    *
    * この一般化をコードで実装してみましょう。
    * incrementとtotalはcommutative monoidを必要とし、mergeはbounded semilattice（またはidempotent commutative monoid）を必要とすることを覚えておいてください。
    *
    * CatsはMonoidとCommutativeMonoidの両方に型クラスを提供していますが、有界半格子には提供していません。
    * そのため、私たちは独自のBoundedSemiLattice型クラスを実装することになります。
    */
  /**
    * 上の実装では、BoundedSemiLattice[A]はCommutativeMonoid[A]を拡張しています。
    * これは、有界半格子が可換単項式（正確には可換偶数項式）であるためです。
    *
    * 11.3.2 Exercise: BoundedSemiLattice Instances
    *
    * IntsとSets用のBoundedSemiLattice型クラス・インスタンスを実装します。
    * Int用のインスタンスは技術的には非負の数に対してのみ有効ですが、型の中で非負を明示的にモデル化する必要はありません。
    */
  import cats.kernel.CommutativeMonoid

  trait BoundedSemiLattice[A] extends CommutativeMonoid[A] {
    def combine(a1: A, a2: A): A
    def empty: A
  }

  object BoundedSemiLattice {
    implicit val intInstance: BoundedSemiLattice[Int] =
      new BoundedSemiLattice[Int] {
        def combine(a1: Int, a2: Int): Int =
          a1 max a2

        val empty: Int =
          0
      }

    implicit def setInstance[A]: BoundedSemiLattice[Set[A]] =
      new BoundedSemiLattice[Set[A]] {
        def combine(a1: Set[A], a2: Set[A]): Set[A] =
          a1 union a2

        val empty: Set[A] =
          Set.empty[A]
      }
  }

  /**
    * 11.3.3 Exercise: Generic GCounter
    *
    * CommutativeMonoidとBoundedSemiLatticeを使用して、GCounterを一般化します。
    *
    * これを実装する際には、Monoid のメソッドや構文を使用して、実装を単純化する機会を探してください。
    * これは、型クラスの抽象化がコードの複数のレベルでどのように機能するかを示す良い例です。
    * 私たちはモノイドを使って大きなコンポーネント（CRDT）を設計していますが、モノイドは小さなコンポーネントにも役立ち、コードを単純化して短く明確にしてくれます。
    *
    * これが実際の実装です。
    * マージの定義に |+| を使用していることに注意してください。
    * これにより、マージとカウンターの最大化のプロセスが大幅に簡素化されます。
    */
//  final case class GCounter[A](counters: Map[String, A]) {
//    import cats.instances.list._ // for Monoid
//    import cats.instances.map._ // for Monoid
//    import cats.syntax.semigroup._ // for |+|
//    import cats.syntax.foldable._ // for combineAll
//    import cats.kernel.CommutativeMonoid
//
//    def increment(machine: String, amount: A)(
//        implicit m: CommutativeMonoid[A]): GCounter[A] = {
//      val value = amount |+| counters.getOrElse(machine, m.empty)
//      GCounter(counters + (machine -> value))
//    }
//
//    def merge(that: GCounter[A])(
//        implicit b: BoundedSemiLattice[A]): GCounter[A] =
//      GCounter(this.counters |+| that.counters)
//
//    def total(implicit m: CommutativeMonoid[A]): A =
//      this.counters.values.toList.combineAll
//
//  }
//  lazy val a = GCounter(Map("A" -> 1)).increment("B",2)
//  println(a)
  /**
    * 11.4 Abstracting GCounter to a Type Class
    *
    * 私たちは、BoundedSemiLatticeとCommutativeMonoidのインスタンスを持つ任意の値で動作する汎用のGCounterを作成しました。
    * しかし、マシンIDから値へのマップの特定の表現に縛られています。
    * このような制限を設ける必要はありませんし、逆に制限を取り除くことができれば便利なこともあります。
    * 単純なマップからリレーショナルデータベースまで、扱いたいキーバリューストアはたくさんあります。
    *
    * GCounter 型のクラスを定義すると、さまざまな具体的な実装を抽象化することができます。
    * これにより、例えば、パフォーマンスと耐久性のトレードオフを変更したい場合に、インメモリストアをパーシステントストアにシームレスに置き換えることができます。
    *
    * これを実装するにはいくつかの方法があります。
    * 一つの方法は、CommutativeMonoidとBoundedSemiLatticeに依存するGCounter型クラスを定義することです。
    * これは、マップの抽象化のキーと値のタイプを表す2つのタイプパラメータを持つタイプコンストラクタを取るタイプクラスとして定義します。
    */
  trait GCounter[F[_, _], K, V] {
    def increment(f: F[K, V])(k: K, v: V)(
        implicit m: CommutativeMonoid[V]): F[K, V]

    def merge(f1: F[K, V], f2: F[K, V])(
        implicit b: BoundedSemiLattice[V]): F[K, V]

    def total(f: F[K, V])(implicit m: CommutativeMonoid[V]): V
  }

  object GCounter {
    def apply[F[_, _], K, V](implicit counter: GCounter[F, K, V]) =
      counter
  }

  /** この型クラスのインスタンスを Map 用に定義してみてください。
    * ケースクラス版GCounterのコードを少し修正して再利用することができるはずです。
    *
    *
    * 以下は、インスタンスの完全なコードです。
    * この定義をGCounterのコンパニオン・オブジェクトに書いて、グローバルな暗黙のスコープに入れます。
    */
  import cats.instances.list._ // for Monoid
  import cats.instances.map._ // for Monoid
  import cats.syntax.semigroup._ // for |+|
  import cats.syntax.foldable._ // for combineAll

  implicit def mapGCounterInstance[K, V]: GCounter[Map, K, V] =
    new GCounter[Map, K, V] {
      def increment(map: Map[K, V])(key: K, value: V)(
          implicit m: CommutativeMonoid[V]): Map[K, V] = {
        val total = map.getOrElse(key, m.empty) |+| value
        map + (key -> total)
      }

      def merge(map1: Map[K, V], map2: Map[K, V])(
          implicit b: BoundedSemiLattice[V]): Map[K, V] =
        map1 |+| map2

      def total(map: Map[K, V])(implicit m: CommutativeMonoid[V]): V =
        map.values.toList.combineAll
    }

  /**
    * きっとインスタンスは以下のように使用できるでしょう
    */
  import cats.instances.int._ // for Monoid

  val g1 = Map("a" -> 7, "b" -> 3)
  val g2 = Map("a" -> 2, "b" -> 5)

  val counter = GCounter[Map, String, Int]

  val merged = counter.merge(g1, g2)
  // merged: Map[String, Int] = Map("a" -> 7, "b" -> 5)
  val total = counter.total(merged)
  // total: Int = 12

  /**
    * 型クラスのインスタンスに対する実装戦略は少し不満です。
    * 実装の構造は、定義したほとんどのインスタンスで同じになりますが、コードの再利用はできません。
    *
    * 11.5 Abstracting a Key Value Store
    *
    * 1つの解決策は、キーバリューストアのアイデアをタイプクラス内に取り込み、キーバリューストアのインスタンスを持つすべてのタイプに対してGCounterインスタンスを生成することです。
    * 以下はそのような型クラスのコードです。
    */
  trait KeyValueStore[F[_, _]] {
    def put[K, V](f: F[K, V])(k: K, v: V): F[K, V]

    def get[K, V](f: F[K, V])(k: K): Option[V]

    def getOrElse[K, V](f: F[K, V])(k: K, default: V): V =
      get(f)(k).getOrElse(default)

    def values[K, V](f: F[K, V]): List[V]
  }

  /**
    * Mapを実装してみてください。
    *
    * これがこのインスタンスのコードです。
    * KeyValueStoreのコンパニオン・オブジェクトに定義を書き込み、グローバルな暗黙のスコープに入れます。
    */
  implicit val mapKeyValueStoreInstance: KeyValueStore[Map] =
    new KeyValueStore[Map] {
      def put[K, V](f: Map[K, V])(k: K, v: V): Map[K, V] =
        f + (k -> v)

      def get[K, V](f: Map[K, V])(k: K): Option[V] =
        f.get(k)

      override def getOrElse[K, V](f: Map[K, V])(k: K, default: V): V =
        f.getOrElse(k, default)

      def values[K, V](f: Map[K, V]): List[V] =
        f.values.toList
    }

  /**
    * 型クラスができたので、インスタンスを持っているデータ型を強化するための構文を実装することができます。
    */
  implicit class KvsOps[F[_, _], K, V](f: F[K, V]) {
    def put(key: K, value: V)(implicit kvs: KeyValueStore[F]): F[K, V] =
      kvs.put(f)(key, value)

    def get(key: K)(implicit kvs: KeyValueStore[F]): Option[V] =
      kvs.get(f)(key)

    def getOrElse(key: K, default: V)(implicit kvs: KeyValueStore[F]): V =
      kvs.getOrElse(f)(key, default)

    def values(implicit kvs: KeyValueStore[F]): List[V] =
      kvs.values(f)
  }

  /**
    * これで、KeyValueStoreとCommutativeMonoidのインスタンスを持つ任意のデータ型に対して、暗黙のDefを使ってGCounterインスタンスを生成することができます。
    */
  implicit def gcounterInstance[F[_, _], K, V](implicit kvs: KeyValueStore[F],
                                               km: CommutativeMonoid[F[K, V]]) =
    new GCounter[F, K, V] {
      def increment(f: F[K, V])(key: K, value: V)(
          implicit m: CommutativeMonoid[V]): F[K, V] = {
        val total = f.getOrElse(key, m.empty) |+| value
        f.put(key, total)
      }

      def merge(f1: F[K, V], f2: F[K, V])(
          implicit b: BoundedSemiLattice[V]): F[K, V] =
        f1 |+| f2

      def total(f: F[K, V])(implicit m: CommutativeMonoid[V]): V =
        f.values.combineAll
    }

  /**
  * このケーススタディの完全なコードはかなり長いのですが、そのほとんどは型クラスに対する操作のための構文を設定する定型的なものです。
  * SimulacrumやKind Projectorのようなコンパイラプラグインを使えば、この部分を削減することができます。
  *
  * 11.6 Summary
  *
  * このケーススタディでは，型クラスを使用して，Scala でシンプルな CRDT である GCounter をどのようにモデル化するかを見てきました．
  * 私たちの実装は，多くの柔軟性とコードの再利用を可能にしています：「カウント」するデータ型にも，マシンIDをカウンタにマッピングするデータ型にも縛られません。
  *
  * このケーススタディでは、Scalaが提供するツールを使用することに焦点を当てており、CRDTを探求することは目的としていません。
  * 他にも多くの CRDT があり、GCounter と似たような動作をするものもあれば、全く異なる実装をするものもあります。
  * かなり最近の調査では、多くの基本的なCRDTの概要がよくわかります。
  * しかし、これは活発な研究分野であり、CRDTや最終的な一貫性に興味がある場合は、この分野の最新の出版物を読むことをお勧めします。
  */

}
