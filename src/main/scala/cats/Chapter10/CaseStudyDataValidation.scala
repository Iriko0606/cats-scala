//package cats.Chapter10
//
//object CaseStudyDataValidation extends App {
//
//  import cats.implicits._
//
//  /**
//    * 今回のケーススタディでは、検証用のライブラリを構築します。
//    * 検証とは何を意味するのでしょうか？
//    * ほとんどのプログラムでは、入力された内容が一定の基準を満たしているかどうかをチェックする必要があります。
//    * ユーザー名は空白であってはならない、メールアドレスは有効でなければならない、などです。
//    * この種の検証は、ウェブフォームで行われることが多いのですが、設定ファイルやウェブサービスの応答など、正しいと保証できないデータを扱わなければならない場合には、すべて検証が行われます。
//    * 例えば、認証は検証の特殊な形です。
//    * これらのチェックを行うライブラリを作りたいと考えています。
//    * どのような設計目標を持つべきでしょうか？インスピレーションを得るために、私たちが行いたいチェックの種類の例を見てみましょう。
//    * - ユーザーは18歳以上であるか、保護者の同意を得ている必要があります。
//    * - 文字列IDはIntとして解析可能であり、そのIntが有効なレコードIDに対応していなければなりません。
//    * - オークションの入札は、1つまたは複数のアイテムに適用され、正の値を持つ必要があります。
//    * - ユーザー名は4文字以上で、すべての文字が英数字でなければなりません。
//    * - メールアドレスには@マークが1つ含まれていなければなりません。
//    * 文字列を@の部分で分割します。左側の文字列は空であってはなりません。
//    * 右側の文字列は3文字以上で、ドットを含む必要があります。
//    *
//    * これらの例を念頭に置いて、いくつかの目標を述べることができます。
//    *
//    * 各検証の失敗に意味のあるメッセージを関連付けることができれば、ユーザーはなぜ自分のデータが有効でないのかを知ることができます。
//    * 小さなチェックをより大きなチェックに組み合わせることができなければなりません。
//    * 先ほどのユーザー名の例で言えば、長さのチェックと英数字のチェックを組み合わせることで、これを表現できるようにすべきです。
//    *
//    * データをチェックしている間に、データを変換することができなければなりません。
//    * 上記の例では、データを解析して、その型をStringからIntに変更しています。
//    *
//    * 最後に、ユーザーが再送信する前にすべての問題を修正できるように、すべての失敗を一度に蓄積することができなければなりません。
//    *
//    * これらの目標は、1つのデータをチェックすることを前提としています。
//    * また、複数のデータをまとめてチェックする必要もあります。
//    * 例えば、ログインフォームでは、ユーザ名とパスワードのチェック結果を組み合わせる必要があります。
//    * これはライブラリの中でもかなり小さなコンポーネントになるので、時間の大半は1つのデータのチェックに集中することになります。
//    *
//    * 10.1 Sketching the Library Structure
//    *
//    * 下から順に、個々のデータを確認していきましょう。
//    * コーディングを始める前に、どのようなものを作るのかをイメージしてみましょう。
//    * そのためには、図式化することが有効です。
//    * 目標を1つずつ確認していきます。
//    *
//    * Providing error messages
//    *
//    * 最初の目標は、チェックが失敗したときに有用なエラーメッセージを関連付けることです。
//    * チェックの出力は、チェックされた値がチェックに合格した場合と、何らかのエラーメッセージの場合があります。
//    * これを抽象的に表現すると、図16に示すように、エラーメッセージの可能性を示すコンテキストの中の値となります。
//    *
//    * Figure 16
//    *
//    * したがって、チェック自体は、図17に示すように、値をコンテキスト内の値に変換する関数です。
//    *
//    * Combine checks
//    *
//    * 小さなチェックを大きなチェックにまとめるには？
//    * これは図18のようにアプリケ-ションなのかセミグル-プなのか？
//    *
//    * Figure 18: Applicative combination of checks
//    *
//    * そうではありません。
//    * 応用的な組み合わせでは、両方のチェックが同じ値に適用され、結果として、その値が繰り返されるタプルができます。
//    * 私たちが求めているのは、図19に示すようなモノイドのようなものです。
//    * 常にパスするチェックという意味のあるアイデンティティーと、2つのバイナリ結合演算子andとorを定義できます。
//    *
//    * Figure 19: Monoid combination of checks
//    *
//    * 検証ライブラリでは、andとorを同じくらいの頻度で使うことになると思いますが、ルールを組み合わせるために2つのモノイドを切り替え続けるのは煩わしいです。
//    * そのため、実際にはモノイドAPIを使用しません。
//    * 代わりに、andとorという2つの別々のメソッドを使用します。
//    *
//    * Accumulating errors as we check
//    *
//    * また、モノイドはエラーメッセージを蓄積するのに適したメカニズムだと感じています。
//    * メッセージをListやNonEmptyListとして保存すれば、Catsの中から既存のモノイドを使うこともできます。
//    *
//    * Transforming data as we check it
//    *
//    * データをチェックすることに加えて、データを変換するという目的もあります。
//    * これは、変換が失敗する可能性があるかどうかによって、mapかflatMapにする必要があるので、図20に示すように、チェックもモナドにしたいようです。
//    *
//    * Figure 20: Monadic combination of checks
//    *
//    * これでライブラリを身近な抽象的なものに分解することができ、開発を始めるのに適した状態になりました。
//    *
//    * 10.2 The Check Datatype
//    *
//    * 私たちのデザインは、チェックを中心に展開しています。
//    * チェックとは、ある文脈の中で、ある値からある値へと変化する機能のことです。
//    * この説明を見た瞬間に、あなたは以下のようなものを思い浮かべるはずです。
//    *
//    * type Check[A] = A => Either[String, A]
//    *
//    * ここでは、エラーメッセージを文字列で表現しています。
//    * これはおそらく最適な表現ではありません。
//    * 例えば、メッセージをリストに蓄積したり、国際化や標準的なエラーコードに対応した別の表現を使用することもできます。
//    *
//    * 私たちは、考えられるすべての情報を保持する何らかのErrorMessageタイプの構築を試みることができます。
//    * しかし、ユーザーの要求を予測することはできません。
//    * 代わりに、ユーザーが欲しいものを指定できるようにしましょう。
//    * これを実現するには、Checkに2つ目のタイプパラメータを追加します。
//    *
//    * type Check[E, A] = A => Either[E, A]
//    *
//    * Checkに独自のメソッドを追加したい場合もあるでしょうから、型の別名ではなく、traitとして宣言しましょう。
//    *
//    * trait Check[E, A] {
//      def apply(value: A): Either[E, A]
//
//      // other methods...
//    }
//    * Essential Scalaで述べたように、traitを定義する際に考慮すべき関数型プログラミングのパターンが2つあります。
//    * - 型クラスにするか，あるいは
//    * - 型クラスにするか，あるいは，代数的なデータ型にする（そして，それゆえに封印する）かです。
//    * 型クラスにすると、異種のデータ型を共通のインターフェースで統一することができます。
//    * これはここでやろうとしていることではないようです。そこで、代数的なデータ型が必要になります。
//    * この考えを念頭に置いて、デザインをもう少し検討してみましょう。
//    *
//    * 10.3 Basic Combinators
//    *
//    * チェックにいくつかのコンビネータ・メソッドを追加してみましょう。
//    * まずは and です。
//    * このメソッドは2つのチェックを1つにまとめ、両方のチェックが成功した場合にのみ成功します。
//    * このメソッドの実装を考えてみましょう。
//    * いくつかの問題にぶつかるはずです。
//    * 問題が起きたら読んでみてください。
//    trait Check[E, A] {
//      def and(that: Check[E, A]): Check[E, A] =
//        ???
//
//      // other methods...
//    }
//    * 問題は、両方のチェックが失敗したときにどうするかです。
//    * 正しいのは両方のエラーを返すことですが、現在のところ、Esを結合する方法はありません。
//    * 図21に示すように、エラーの「累積」という概念を抽象化した型クラスが必要です。
//    *
//    * Figure 21: Combining error messages
//    *
//    * EのSemigroupが必要です。
//    * そして、combineメソッドやそれに関連する｜+｜構文を使ってEの値を組み合わせることができます。
//    *
//    */
//  import cats.Semigroup
//  import cats.instances.list._ // for Semigroup
//  import cats.syntax.semigroup._ // for |+|
//
//  val semigroup = Semigroup[List[String]]
//
//  // Combination using methods on Semigroup
//  semigroup.combine(List("Badness"), List("More badness"))
//  // res3: List[String] = List("Badness", "More badness")
//
//  // Combination using Semigroup syntax
//  List("Oh noes") |+| List("Fail happened")
//  // res4: List[String] = List("Oh noes", "Fail happened")
//
//  /**
//    * ID要素を必要としないため、完全なMonoidを必要としないことに注意してください。
//    * Id => 単位元
//    * 私たちは常に、制約をできるだけ小さくするようにしなければなりません。
//    *
//    * 最初のチェックが失敗した場合、ショートさせるべきかどうかという、意味的な問題もすぐに出てきます。
//    * 最も便利な動作は何だと思いますか？
//    *
//    * 私たちはできる限りすべてのエラーを報告したいので、可能な限りショートしないようにすべきです。
//    *
//    * andメソッドの場合、組み合わせている2つのチェックはお互いに独立しています。
//    * いつでも両方のルールを実行して、見つかったエラーを組み合わせることができます。
//    *
//    * この知識を使って、andを実行し 期待通りの挙動で終われるようにしましょう!
//    *
//    * 少なくとも2つの実装方法があります。
//    * 1つ目は、チェックを関数として表現する方法です。
//    * Check データ型は、コンビネータのメソッドを提供する関数の単純なラッパーとなります。
//    * 曖昧さをなくすために、この実装をCheckFと呼ぶことにします。
//    */
////  import cats.Semigroup
////  import cats.syntax.either._ // for asLeft and asRight
////  import cats.syntax.semigroup._ // for |+|
////
////  final case class CheckF[E, A](func: A => Either[E, A]) {
////    def apply(a: A): Either[E, A] =
////      func(a)
////
////    def and(that: CheckF[E, A])(implicit s: Semigroup[E]): CheckF[E, A] =
////      CheckF { a =>
////        (this(a), that(a)) match {
////          case (Left(e1), Left(e2)) => (e1 |+| e2).asLeft
////          case (Left(e), Right(_))  => e.asLeft
////          case (Right(_), Left(e))  => e.asLeft
////          case (Right(_), Right(_)) => a.asRight
////        }
////      }
////  }
//
//  /**
//    * それでは、実際に動作を確認してみましょう。まず、いくつかのチェック項目を設定します。
//    */
////  import cats.instances.list._ // for Semigroup
////
////  val a: CheckF[List[String], Int] =
////    CheckF { v =>
////      if (v > 2) v.asRight
////      else List("Must be > 2").asLeft
////    }
////
////  val b: CheckF[List[String], Int] =
////    CheckF { v =>
////      if (v < -2) v.asRight
////      else List("Must be < -2").asLeft
////    }
////
////  val check: CheckF[List[String], Int] =
////    a and b
//
//  /**
//    * それでは、いくつかのデータを使ってチェックを行ってみましょう。
//    */
////  check(5)
//  // res5: Either[List[String], Int] = Left(List("Must be < -2"))
////  check(0)
//  // res6: Either[List[String], Int] = Left(List("Must be > 2", "Must be < -2"))
//
//  /**
//    * 素晴らしいです。全てが期待通りに動いています! 両方のチェックを実行し、必要に応じてエラーを蓄積しています。
//    * もし、蓄積できない型で失敗するチェックを作成しようとするとどうなるでしょうか？
//    * 例えば、NothingのSemigroupインスタンスはありません。
//    * CheckF[Nothing, A]のインスタンスを作成するとどうなるでしょうか？
//    */
////  val a: CheckF[Nothing, Int] =
////    CheckF(v => v.asRight)
////
////  val b: CheckF[Nothing, Int] =
////    CheckF(v => v.asRight)
//
//  /**
//    * チェックを作成するのは問題ないのですが、それを組み合わせるとなると、予想通りのエラーが発生します。
//    */
//  //val check = a and b
//  // error: could not find implicit value for parameter s: cats.Semigroup[Nothing]
//  //   a and b
//  //   ^^^^^^^
//  /**
//    * チェックを作成することはできますが、組み合わせようとすると、予想されるエラーが発生します。
//    */
//  /**
//    * では、別の実装方法を見てみましょう。
//    * この方法では，チェックを代数的なデータ型としてモデル化し，各コンビネータに対して明示的なデータ型を用意します．
//    * この実装をCheckと呼ぶことにしましょう。
//    */
////  import cats.Semigroup
////  import cats.data.Validated
////  import cats.syntax.semigroup._ // for |+|
////  import cats.syntax.apply._ // for mapN
////  import cats.data.Validated._ // for Valid and Invalid
////
////  sealed trait Check[E, A] {
////    import Check._
////
////    def and(that: Check[E, A]): Check[E, A] =
////      And(this, that)
////
////    def or(that: Check[E, A]): Check[E, A] =
////      Or(this, that)
////
////    def apply(a: A)(implicit s: Semigroup[E]): Validated[E, A] =
////      this match {
////        case Pure(func) =>
////          func(a)
////
////        case And(left, right) =>
////          (left(a), right(a)).mapN((_, _) => a)
////
////        case Or(left, right) =>
////          left(a) match {
////            case Valid(a) => Valid(a)
////            case Invalid(e1) =>
////              right(a) match {
////                case Valid(a)    => Valid(a)
////                case Invalid(e2) => Invalid(e1 |+| e2)
////              }
////          }
////      }
////  }
////
////  object Check {
////    final case class And[E, A](left: Check[E, A], right: Check[E, A])
////        extends Check[E, A]
////
////    final case class Or[E, A](left: Check[E, A], right: Check[E, A])
////        extends Check[E, A]
////
////    final case class Pure[E, A](func: A => Validated[E, A]) extends Check[E, A]
////  }
////
////  /**
////    * 一例を見てみましょう。
////    */
////  val a: Check[List[String], Int] =
////    Check.pure { v =>
////      if (v > 2) v.asRight
////      else List("Must be > 2").asLeft
////    }
////
////  val b: Check[List[String], Int] =
////    Check.pure { v =>
////      if (v < -2) v.asRight
////      else List("Must be < -2").asLeft
////    }
////
////  val check: Check[List[String], Int] =
////    a and b
//
//  /**
//    * ADTの実装は、関数ラッパーの実装よりも冗長ですが、計算の構造（作成したADTインスタンス）と、それに意味を与えるプロセス（applyメソッド）をきれいに分離できるという利点があります。
//    * ここからは、いくつかの選択肢があります。
//    * - チェックが作成された後、検査とリファクタリングを行う。
//    * - applyの「インタープリタ」を独自のモジュールに移行しました。
//    * - 他の機能(例えば、チェックの視覚化)を提供する代替インタープリタを実装する。
//    * その柔軟性から、このケーススタディの残りの部分ではADTの実装を使用します。
//    *
//    * 厳密に言えば、Either[E, A]は、我々のチェックの出力としては間違った抽象化です。
//    * なぜこのようなことが起こるのでしょうか？
//    * 代わりにどんなデータ型を使えばいいのでしょうか？
//    * 実装をこの新しいデータ型に切り替えてみてください。
//    *
//    * Andに対するapplyの実装は、applicative functorsのパターンを使っています。
//    * EitherはApplicativeのインスタンスを持っていますが、私たちが望むセマンティクスを持っていません。
//    * エラーを蓄積するのではなく、すぐに失敗してしまいます。
//    *
//    * これが完全な実装です。
//    **/
////  import cats.Semigroup
////  import cats.data.Validated
////  import cats.syntax.apply._ // for mapN
////
////  sealed trait Check[E, A] {
////    import Check._
////
////    def and(that: Check[E, A]): Check[E, A] =
////      And(this, that)
////
////    def apply(a: A)(implicit s: Semigroup[E]): Validated[E, A] =
////      this match {
////        case Pure(func) =>
////          func(a)
////
////        case And(left, right) =>
////          (left(a), right(a)).mapN((_, _) => a)
////      }
////  }
////  object Check {
////    final case class And[E, A](left: Check[E, A], right: Check[E, A])
////        extends Check[E, A]
////
////    final case class Pure[E, A](func: A => Validated[E, A]) extends Check[E, A]
////  }
//
//  /** もしエラーを蓄積したいのであれば、Validatedの方がより適切な抽象化です。
//    * おまけに、applyの実装でValidatedのアプリケーティブなインスタンスを利用できるので、コードの再利用性が高まります。
//    */
//  /**
//    * 実装はかなりいい感じになっています。
//    * andを補完するorコンビネーターを実装します。
//    *
//    * これは、andに対しても同じ手法を再利用しています。applyメソッドでもう少し作業をしなければなりません。
//    * ルールの選択は "or "のセマンティクスに暗黙的に含まれているので、この場合は短絡的に考えても問題ないことに注意してください。
//    */
////  import cats.Semigroup
////  import cats.data.Validated
////  import cats.syntax.semigroup._ // for |+|
////  import cats.syntax.apply._ // for mapN
////  import cats.data.Validated._ // for Valid and Invalid
////
////  sealed trait Check[E, A] {
////    import Check._
////
////    def and(that: Check[E, A]): Check[E, A] =
////      And(this, that)
////
////    def or(that: Check[E, A]): Check[E, A] =
////      Or(this, that)
////
////    def apply(a: A)(implicit s: Semigroup[E]): Validated[E, A] =
////      this match {
////        case Pure(func) =>
////          func(a)
////
////        case And(left, right) =>
////          (left(a), right(a)).mapN((_, _) => a)
////
////        case Or(left, right) =>
////          left(a) match {
////            case Valid(a) => Valid(a)
////            case Invalid(e1) =>
////              right(a) match {
////                case Valid(a)    => Valid(a)
////                case Invalid(e2) => Invalid(e1 |+| e2)
////              }
////          }
////      }
////  }
////  object Check {
////    final case class And[E, A](left: Check[E, A], right: Check[E, A])
////        extends Check[E, A]
////
////    final case class Or[E, A](left: Check[E, A], right: Check[E, A])
////        extends Check[E, A]
////
////    final case class Pure[E, A](func: A => Validated[E, A]) extends Check[E, A]
////  }
//
//  /**
//    * and and or を使えば、実際に必要となる多くのチェックを実装することができます。
//    * しかし、まだいくつかのメソッドを追加する必要があります。次はmapとその関連メソッドについて説明します。
//    */
//  /**
//    * 10.4 Transforming Data
//    *
//    * 私たちの要求の一つは、データを変換する機能です。
//    * これにより、入力を解析するような追加のシナリオをサポートすることができます。
//    * このセクションでは、チェックライブラリにこの追加機能を追加します。
//    * わかりやすい出発点はマップです。
//    * これを実装しようとすると、すぐに壁にぶつかります。
//    * 現在のチェックの定義では、入力タイプと出力タイプが同じであることが要求されています。
//    */
////  type Check[E, A] = A => Either[E, A]
//
//  /**
//    * チェックを重ねるとき、その結果にどのようなタイプを割り当てるか。AでもなくBでもなく、袋小路に入ってしまいます。
//    */
//  // def map(check: Check[E, A])(func: A => B): Check[E, ???]
//
//  /**
//    * マップを実装するには、Checkの定義を変更する必要があります。
//    * 具体的には、入力の型と出力の型を分けるために、新しい型変数が必要になります。
//    */
////  type Check[E, A, B] = A => Either[E, B]
//
//  /**
//    * チェックは、StringをIntとして解析するような操作を表現できるようになりました。
//    */
////  val parseInt: Check[List[String], String, Int] =
////  // etc...
//
//  /**
//    * しかし、入力タイプと出力タイプを分けることで、別の問題が発生します。
//    * これまで私たちは、Checkは成功したら必ず入力を返すという前提で操作してきました。
//    * これをandやorで利用して、左右のルールの出力を無視し、成功したら元の入力を返すだけにしました。
//    */
////  (this(a), that(a)) match {
////    case And(left, right) =>
////      (left(a), right(a))
////        .mapN((result1, result2) => Right(a))
////
////    // etc...
////  }
//  /**
//    * 新しい形式では、Right(a)を返すことができません。
//    * なぜなら、その型はEither[E, A]であり、Either[E, B]ではないからです。Right(result1)を返すか、Right(result2)を返すか、恣意的な選択をせざるを得ないのです。
//    * orメソッドでも同じことが言えます。
//    * ここから、2つのことが導き出せます。
//    * - 遵守する法律を明示するよう努力すべきである。
//    * - コードが教えてくれるのは、Checkの抽象化が間違っているということです。
//    */
//  /**
//    * 10.4.1 Predicates
//    *
//    * andやorなどの論理演算で組み合わせることができる「述語」の概念と、データを変換することができる「チェック」の概念を分けて考えることで、前進することができます。
//    * これまでチェックと呼んでいたものをプレディケートと呼ぶことにします。
//    * プレディケートについては、「プレディケートは成功すれば必ず入力を返す」という概念をコード化した次のような恒等法を述べることができます。
//    */
////  For a predicate p of type Predicate[E, A] and elements a1 and a2 of type A, if p(a1) == Success(a2) then a1 == a2.
//
//  /**
//    * この変更により、以下のようなコードになります。
//    */
//  import cats.Semigroup
//  import cats.data.Validated
//  import cats.syntax.semigroup._ // for |+|
//  import cats.syntax.apply._ // for mapN
//  import cats.data.Validated._ // for Valid and Invalid
//
////  sealed trait Predicate[E, A] {
////    def and(that: Predicate[E, A]): Predicate[E, A] =
////      And(this, that)
////
////    def or(that: Predicate[E, A]): Predicate[E, A] =
////      Or(this, that)
////
////    def apply(a: A)(implicit s: Semigroup[E]): Validated[E, A] =
////      this match {
////        case Pure(func) =>
////          func(a)
////
////        case And(left, right) =>
////          (left(a), right(a)).mapN((_, _) => a)
////
////        case Or(left, right) =>
////          left(a) match {
////            case Valid(_) => Valid(a)
////            case Invalid(e1) =>
////              right(a) match {
////                case Valid(_)    => Valid(a)
////                case Invalid(e2) => Invalid(e1 |+| e2)
////              }
////          }
////      }
////  }
//
////  final case class And[E, A](left: Predicate[E, A], right: Predicate[E, A])
////      extends Predicate[E, A]
////
////  final case class Or[E, A](left: Predicate[E, A], right: Predicate[E, A])
////      extends Predicate[E, A]
////
////  final case class Pure[E, A](func: A => Validated[E, A])
////      extends Predicate[E, A]
//
//  /**
//    * 10.4.2 Checks
//    *
//    * ここではCheckを使って、入力の変換も可能なPredicateから構築する構造を表現します。
//    * 以下のインターフェイスでCheckを実装します。
//    *
//    sealed trait Check[E, A, B] {
//      def apply(a: A): Validated[E, B] = ???
//      def map[C](func: B => C): Check[E, A, C] = ???
//    }
//    */
//  import cats.Semigroup
//  import cats.data.Validated
//
////  sealed trait Check[E, A, B] {
////    import Check._
////    def apply(in: A)(implicit s: Semigroup[E]): Validated[E, B]
////
////    def map[C](f: B => C): Check[E, A, C] =
////      Map[E, A, B, C](this, f)
////  }
////
////  object Check {
////    final case class Map[E, A, B, C](check: Check[E, A, B], func: B => C)
////        extends Check[E, A, C] {
////
////      def apply(in: A)(implicit s: Semigroup[E]): Validated[E, C] =
////        check(in).map(func)
////    }
////
////    final case class Pure[E, A](pred: Predicate[E, A]) extends Check[E, A, A] {
////
////      def apply(in: A)(implicit s: Semigroup[E]): Validated[E, A] =
////        pred(in)
////    }
////
////    def apply[E, A](pred: Predicate[E, A]): Check[E, A, A] =
////      Pure(pred)
////  }
//
//  /**
//    * flatMapはどうでしょうか？ここでは、セマンティクスが少し不明瞭です。
//    * このメソッドは宣言するだけなら簡単ですが、それが何を意味しているのか、どのようにしてアプリケーションを実装すべきなのかは、あまり明らかではありません。
//    * flatMapの一般的な形を図22に示す。
//    *
//    * Figure 22: Type chart for flatMap
//    *
//    * 図のFとコードのCheckをどのように関連付けるのか？Checkには3つの型変数がありますが、Fには1つしかありません。
//    *
//    * 型を統一するには、型変数のうち2つを固定する必要があります。
//    * エラー型Eと入力型Aを選択すると、図23のような関係になります。
//    * 言い換えると、FlatMapを適用するセマンティクスは次のようになります。
//    *
//    * - タイプAの入力があると、F[B]に変換する。
//    * - タイプBの出力を使って、Check[E, A, C]を選択する。
//    * - タイプAの元の入力に戻り、それを選択したチェックに適用して、タイプF[C]の最終結果を生成する。
//    *
//    * Figure 23: Type chart for flatMap applied to Check
//    *
//    * これはかなり変わった方法です。
//    * 実装することはできますが、使い道を見つけるのは難しいです。
//    * CheckのためにflatMapを実装してみてください。
//    * そうすれば、もっと一般的に有用なメソッドを見ることができるでしょう。
//    *
//    * これまでと同じ実装方法ですが、一つだけ気になる点があります。
//    * ValidatedにはflatMapメソッドがないのです。
//    * flatMapを実装するためには、一旦Eitherに切り替えてから、Validatedに戻す必要があります。
//    * ValidatedのwithEitherメソッドがまさにこれを行います。
//    * ここからは、型を追ってapplyを実装していくことになります。
//    */
//  import cats.Semigroup
//  import cats.data.Validated
//
//  sealed trait Check2[E, A, B] {
//    def apply(in: A)(implicit s: Semigroup[E]): Validated[E, B]
//
//    def flatMap[C](f: B => Check2[E, A, C]) =
//      FlatMap[E, A, B, C](this, f)
//
//    // other methods...
//  }
//
//  final case class FlatMap[E, A, B, C](check: Check2[E, A, B],
//                                       func: B => Check2[E, A, C])
//      extends Check2[E, A, C] {
//
//    def apply(a: A)(implicit s: Semigroup[E]): Validated[E, C] =
//      check(a).withEither(_.flatMap(b => func(b)(a).toEither))
//  }
//
//  // other data types...
//
//  /**
//    * もっと便利なコンビネータを書けば、2つのチェックを連鎖させることができます。
//    * 最初のチェックの出力は，2番目のチェックの入力に接続されます．
//    * これは andThen を使った関数合成に似ています。
//    *
//    * val f: A => B = ???
//    * val g: B => C = ???
//    * val h: A => C = f andThen g
//    *
//    * Checkは基本的に関数A => Validated[E, B]なので、類似したandThenメソッドを定義できます。
//    */
////  trait Check3[E, A, B] {
////    def andThen[C](that: Check[E, B, C]): Check[E, A, C]
////  }
//
//  /**
//    * andThenを実装しましょう！
//    */
//  sealed trait Check3[E, A, B] {
//    def apply(in: A)(implicit s: Semigroup[E]): Validated[E, B]
//
//    def andThen[C](that: Check3[E, B, C]): Check3[E, A, C] =
//      AndThen[E, A, B, C](this, that)
//  }
//
//  final case class AndThen[E, A, B, C](check1: Check3[E, A, B],
//                                       check2: Check3[E, B, C])
//      extends Check3[E, A, C] {
//
//    def apply(a: A)(implicit s: Semigroup[E]): Validated[E, C] =
//      check1(a).withEither(_.flatMap(b => check2(b).toEither))
//  }
//
//  /**
//    * 10.4.3 Recap
//    *
//    * これで、PredicateとCheckという2つの代数的なデータ型と、関連するcaseクラスの実装を持つ多数のコンビネータが揃いました。
//    * 各ADTの完全な定義については、次のソリューションをご覧ください。
//    *
//    * これが、コードの整理と再パッケージ化を含む、私たちの最終的なインプリメンテーションです。
//    */
//  import cats.Semigroup
//  import cats.data.Validated
//  import cats.data.Validated._ // for Valid and Invalid
//  import cats.syntax.semigroup._ // for |+|
//  import cats.syntax.apply._ // for mapN
//  import cats.syntax.validated._ // for valid and invalid
//
//  /**
//    * ここでは、and および or コンビネータや、関数から Predicate を作成する Predicate.apply メソッドを含む、Predicate の完全な実装を紹介します。
//    */
//  sealed trait Predicate[E, A] {
//    import Predicate._
//    import Validated._
//
//    def and(that: Predicate[E, A]): Predicate[E, A] =
//      And(this, that)
//
//    def or(that: Predicate[E, A]): Predicate[E, A] =
//      Or(this, that)
//
//    def apply(a: A)(implicit s: Semigroup[E]): Validated[E, A] =
//      this match {
//        case Pure(func) =>
//          func(a)
//
//        case And(left, right) =>
//          (left(a), right(a)).mapN((_, _) => a)
//
//        case Or(left, right) =>
//          left(a) match {
//            case Valid(_) => Valid(a)
//            case Invalid(e1) =>
//              right(a) match {
//                case Valid(_)    => Valid(a)
//                case Invalid(e2) => Invalid(e1 |+| e2)
//              }
//          }
//      }
//  }
//
//  object Predicate {
//    final case class And[E, A](left: Predicate[E, A], right: Predicate[E, A])
//        extends Predicate[E, A]
//
//    final case class Or[E, A](left: Predicate[E, A], right: Predicate[E, A])
//        extends Predicate[E, A]
//
//    final case class Pure[E, A](func: A => Validated[E, A])
//        extends Predicate[E, A]
//
//    def apply[E, A](f: A => Validated[E, A]): Predicate[E, A] =
//      Pure(f)
//
//    def lift[E, A](err: E, fn: A => Boolean): Predicate[E, A] =
//      Pure(a => if (fn(a)) a.valid else err.invalid)
//  }
//
//  /**
//    * ここでは，Checkの完全な実装を紹介します．
//    * Scalaのパターンマッチングには型推論のバグがあるため、継承を使ったapplyの実装に切り替えました。
//    */
//  import cats.Semigroup
//  import cats.data.Validated
//  import cats.syntax.apply._ // for mapN
//  import cats.syntax.validated._ // for valid and invalid
//
//  sealed trait Check[E, A, B] {
//    import Check._
//
//    def apply(in: A)(implicit s: Semigroup[E]): Validated[E, B]
//
//    def map[C](f: B => C): Check[E, A, C] =
//      Map[E, A, B, C](this, f)
//
//    def flatMap[C](f: B => Check[E, A, C]) =
//      FlatMap[E, A, B, C](this, f)
//
//    def andThen[C](next: Check[E, B, C]): Check[E, A, C] =
//      AndThen[E, A, B, C](this, next)
//  }
//
//  object Check {
//    final case class Map[E, A, B, C](check: Check[E, A, B], func: B => C)
//        extends Check[E, A, C] {
//
//      def apply(a: A)(implicit s: Semigroup[E]): Validated[E, C] =
//        check(a) map func
//    }
//
//    final case class FlatMap[E, A, B, C](check: Check[E, A, B],
//                                         func: B => Check[E, A, C])
//        extends Check[E, A, C] {
//
//      def apply(a: A)(implicit s: Semigroup[E]): Validated[E, C] =
//        check(a).withEither(_.flatMap(b => func(b)(a).toEither))
//    }
//
//    final case class AndThen[E, A, B, C](check: Check[E, A, B],
//                                         next: Check[E, B, C])
//        extends Check[E, A, C] {
//
//      def apply(a: A)(implicit s: Semigroup[E]): Validated[E, C] =
//        check(a).withEither(_.flatMap(b => next(b).toEither))
//    }
//
//    final case class Pure[E, A, B](func: A => Validated[E, B])
//        extends Check[E, A, B] {
//
//      def apply(a: A)(implicit s: Semigroup[E]): Validated[E, B] =
//        func(a)
//    }
//
//    final case class PurePredicate[E, A](pred: Predicate[E, A])
//        extends Check[E, A, A] {
//
//      def apply(a: A)(implicit s: Semigroup[E]): Validated[E, A] =
//        pred(a)
//    }
//
//    def apply[E, A](pred: Predicate[E, A]): Check[E, A, A] =
//      PurePredicate(pred)
//
//    def apply[E, A, B](func: A => Validated[E, B]): Check[E, A, B] =
//      Pure(func)
//  }
//
//  /**
//    * CheckとPredicateの実装が完成し、当初の目的のほとんどを果たすことができました。
//    * しかし、まだ完成ではありません。
//    * おそらく、PredicateとCheckには抽象化できる構造があることを認識しているでしょう。
//    * Predicateにはモノイドがあり、Checkにはモナドがあります。
//    * さらに、Checkの実装では、PredicateやValidatedの基本的なメソッドを呼び出しているだけで、実装があまり機能していないと感じたかもしれません。
//    *
//    * このライブラリをきれいにする方法はたくさんあります。
//    * しかし、いくつかの例を実装して、このライブラリが本当に動作することを証明してから、改良に取り掛かりましょう。
//    *
//    * 序章で述べた例のいくつかについて、チェックを実装してみましょう。
//    *
//    * - ユーザ名は4文字以上の英数字のみで構成されていること
//    * - メールアドレスに@マークが含まれていること。
//    * - 文字列を@の部分で分割します。左側の文字列は空であってはなりません。
//    * - 右側の文字列は、少なくとも3文字以上で、ドットを含んでいなければなりません。
//    *
//    * 次のような述語が役に立つかもしれません。
//    */
//  import cats.data.{NonEmptyList, Validated}
//
//  type Errors = NonEmptyList[String]
//
//  def error(s: String): NonEmptyList[String] =
//    NonEmptyList(s, Nil)
//
//  def longerThan(n: Int): Predicate[Errors, String] =
//    Predicate.lift(error(s"Must be longer than $n characters"),
//                   str => str.size > n)
//
//  val alphanumeric: Predicate[Errors, String] =
//    Predicate.lift(error(s"Must be all alphanumeric characters"),
//                   str => str.forall(_.isLetterOrDigit))
//
//  def contains(char: Char): Predicate[Errors, String] =
//    Predicate.lift(error(s"Must contain the character $char"),
//                   str => str.contains(char))
//
//  def containsOnce(char: Char): Predicate[Errors, String] =
//    Predicate.lift(error(s"Must contain the character $char only once"),
//                   str => str.filter(c => c == char).size == 1)
//
//  /**
//    * これが私たちの参考になるソリューションです。
//    * これを実装するには、予想以上に考える必要がありました。
//    * CheckとPredicateを適切な場所で切り替えることは、Predicateは入力を変換しないというルールを頭に入れるまでは、ちょっとした当て推量のように感じました。
//    * このルールを念頭に置くことで、物事はかなりスムーズに進みました。
//    * 後のセクションでは、このライブラリをより使いやすくするためにいくつかの変更を行います。
//    */
//  import cats.syntax.apply._ // for mapN
//  import cats.syntax.validated._ // for valid and invalid
//
//  /**
//    * ここではcheckUsernameの実装を紹介します。
//    */
//  // A username must contain at least four characters
//  // and consist entirely of alphanumeric characters
//
//  val checkUsername: Check[Errors, String, String] =
//    Check(longerThan(3) and alphanumeric)
//
//  /**
//    * そして、いくつかの小さなコンポーネントから構成されたcheckEmailの実装です。
//    */
//  // An email address must contain a single `@` sign.
//  // Split the string at the `@`.
//  // The string to the left must not be empty.
//  // The string to the right must be
//  // at least three characters long and contain a dot.
//
//  val splitEmail: Check[Errors, String, (String, String)] =
//    Check(_.split('@') match {
//      case Array(name, domain) =>
//        (name, domain).validNel[String]
//
//      case _ =>
//        "Must contain a single @ character".invalidNel[(String, String)]
//    })
//
//  val checkLeft: Check[Errors, String, String] =
//    Check(longerThan(0))
//
//  val checkRight: Check[Errors, String, String] =
//    Check(longerThan(3) and contains('.'))
//
//  val joinEmail: Check[Errors, (String, String), String] = Check {
//    case (l, r) =>
//      (checkLeft(l), checkRight(r)).mapN(_ + "@" + _)
//  }
//
//  val checkEmail: Check[Errors, String, String] =
//    splitEmail andThen joinEmail
//
//  /**
//    * 最後に、checkUsernameとcheckEmailに依存するUserのチェックを紹介します。
//    */
//  final case class User(username: String, email: String)
//
//  def createUser(username: String, email: String): Validated[Errors, User] =
//    (checkUsername(username), checkEmail(email)).mapN(User)
//
//  /**
//    * いくつかのサンプルユーザーを作成することで、私たちの作業を確認することができます。
//    */
//  createUser("Noel", "noel@underscore.io")
//  // res5: Validated[Errors, User] = Valid(User("Noel", "noel@underscore.io"))
//  createUser("", "dave@underscore.io@io")
//  // res6: Validated[Errors, User] = Invalid(
//  //   NonEmptyList(
//  //     "Must be longer than 3 characters",
//  //     List("Must contain a single @ character")
//  //   )
//  // )
//  /**
//    * この例の明確な欠点は、エラーがどこから来たのかわからないことです。
//    * エラーメッセージを適切に操作することでこの問題を解決するか、メッセージだけでなくエラー発生場所を追跡するようにライブラリを変更することができます。
//    * エラーの場所を追跡することは、今回のケーススタディの範囲外ですので、読者の皆様には練習問題としてお任せします。
//    *
//    * 10.5 Kleislis
//    *
//    * このケーススタディの最後に、Checkの実装を整理します。
//    * 私たちのアプローチに対する正当な批判は、ほとんど何もしないのにたくさんのコードを書いているということです。
//    * プレディケートは基本的に関数A => Validated[E, A]であり、チェックは基本的にこれらの関数を合成するためのラッパーです。
//    *
//    * A => Validated[E, A]をA => F[B]と抽象化することができます。
//    * これは、モナドのflatMapメソッドに渡す関数の種類として認識されるでしょう。
//    * 以下のような一連の操作があるとします。
//    *
//    * ある値を（例えばpureを使って）モナドに取り込みます。これは、A => F[A] という型の関数です。
//    *
//    * 次に、flatMapを使ってモナドにいくつかの変換を行います。
//    *
//    * これを図24のように説明することができます。また、この例をモナドAPIを使って以下のように書き出すこともできる。
//    *
//  val aToB: A => F[B] = ???
//  val bToC: B => F[C] = ???
//
//  def example[A, C](a: A): F[C] = aToB(a).flatMap(bToC)
//
//    * Figure 24: Sequencing monadic transforms
//    * Checkは、抽象的には、A => F[B]という型の関数を合成できるようにしていることを思い出してください。
//    * 上記をandThenの観点から次のように書くことができます。
//
//  val aToC = aToB andThen bToC
//
//    * 結果は、A => F[C]型の（ラップされた）関数 aToC で、これを後から A 型の値に適用することができます。
//    *
//    * CheckのandThenメソッドは、関数の合成と似ていますが、A => Bではなく、関数A => F[B]を合成しています。
//    *
//    * A => F[B]型の関数を合成するという抽象的な概念には、クライスリという名前がついています。
//    *
//    * Cats には cats.data.Kleisli というデータ型があり、Check と同様に関数をラップします。
//    * KleisliはCheckのすべてのメソッドに加え、いくつかの追加メソッドを持っています。
//    * Kleisliに見覚えがある方は、おめでとうございます。
//    * あなたはその変装を見破り、この本の序盤に出てきた別の概念であることを認識しました。KleisliはReaderTの別名なのです。
//    *
//    * ここでは、Kleisliを使って、3つのステップで整数を整数のリストに変換する簡単な例を紹介します。
//    */
//  import cats.data.Kleisli
//  import cats.instances.list._ // for Monad
//
//  /**
//    * これらのステップはそれぞれ、入力IntをList[Int]型の出力に変換します。
//    */
//  val step1: Kleisli[List, Int, Int] =
//    Kleisli(x => List(x + 1, x - 1))
//
//  val step2: Kleisli[List, Int, Int] =
//    Kleisli(x => List(x, -x))
//
//  val step3: Kleisli[List, Int, Int] =
//    Kleisli(x => List(x * 2, x / 2))
//
//  /**
//    * これらのステップを、flatMapを使って基礎となるListsを結合する単一のパイプラインにまとめることができます。
//    */
//  val pipeline = step1 andThen step2 andThen step3
//
//  /**
//    * その結果、1つのIntを消費し、8つの出力を返す関数となります。
//    * それぞれの出力は、step1、step2、step3の変換の異なる組み合わせによって生成されます。
//    */
//  pipeline.run(20)
//  // res0: List[Int] = List(42, 10, -42, -10, 38, 9, -38, -9)
//
//  /**
//    * APIの面でKleisliとCheckの唯一の注目すべき違いは、Kleisliがapplyメソッドの名前をrunに変更していることです。
//    *
//    * それでは、検証の例でCheckをKleisliに置き換えてみましょう。
//    * そのためには、Predicate にいくつかの変更を加える必要があります。
//    * Kleisliは関数でのみ動作するので、Predicateを関数に変換できなければなりません。
//    * 少し微妙なことですが、Predicateを関数に変換する際には、A => Validated[E, A]ではなく、A => Either[E, A]という型にしなければなりません。
//    *
//    * 正しい型の関数を返すrunというメソッドをPredicateに追加します。
//    * Predicateの他のコードはそのままにしておきます。
//    *
//    * ここでは、runの略式の定義を示します。applyと同様に、このメソッドは暗黙のSemigroupを受け入れる必要があります。
//    */
//  import cats.Semigroup
//  import cats.data.Validated
//
//  sealed trait Predicate[E, A] {
//    def run(implicit s: Semigroup[E]): A => Either[E, A] =
//      (a: A) => this(a).toEither
//
//    def apply(a: A): Validated[E, A] =
//      ??? // etc...
//
//    // other methods...
//  }
//
//  /**
//    * それでは、ユーザー名とメールアドレスの検証例を、クライスラーとプレディケートの観点から書き直してみましょう。
//    * 行き詰まったときのために、いくつかのヒントを紹介します。
//    *
//    * まず、Predicateのrunメソッドは、暗黙のパラメータを取ることを覚えておいてください。
//    * aPredicate.run(a)を呼び出すと、暗黙のパラメータを明示的に渡そうとします。
//    * もし、Predicateから関数を生成して、すぐにその関数を適用したい場合は、aPredicate.run.apply(a)を使います。
//    *
//    * 次に、この演習では型推論が厄介です。
//    * 以下のような定義をすることで、型宣言の少ないコードを書くことができることがわかりました。
//    */
//  type Result[A] = Either[Errors, A]
//
//  type Check[A, B] = Kleisli[Result, A, B]
//
//  // Create a check from a function:
//  def check[A, B](func: A => Result[B]): Check[A, B] =
//    Kleisli(func)
//
//  // Create a check from a Predicate:
//  def checkPred[A](pred: Predicate[Errors, A]): Check[A, A] =
//    Kleisli[Result, A, A](pred.run)
//
//  /**
//    * 型推論の制限を回避することは、このコードを書いているときに非常にイライラすることがあります。
//    * Predicates、functions、Validated、Eitherの間で変換するタイミングを見つけ出すことで、物事を単純化することができますが、プロセスはまだ複雑です。
//    */
//  import cats.data.{Kleisli, NonEmptyList}
//  import cats.instances.either._ // for Semigroupal
//
//  /**
//    * ここでは、ケーススタディの本文で提案した前文を紹介します。
//    */
//  type Errors = NonEmptyList[String]
//
//  def error(s: String): NonEmptyList[String] =
//    NonEmptyList(s, Nil)
//
//  type Result[A] = Either[Errors, A]
//
//  type Check[A, B] = Kleisli[Result, A, B]
//
//  def check[A, B](func: A => Result[B]): Check[A, B] =
//    Kleisli(func)
//
//  def checkPred[A](pred: Predicate[Errors, A]): Check[A, A] =
//    Kleisli[Result, A, A](pred.run)
//
//  /**
//    * 基本的な述語の定義は基本的に変更されません。
//    */
//  def longerThan(n: Int): Predicate[Errors, String] =
//    Predicate.lift(error(s"Must be longer than $n characters"),
//                   str => str.size > n)
//
//  val alphanumeric: Predicate[Errors, String] =
//    Predicate.lift(error(s"Must be all alphanumeric characters"),
//                   str => str.forall(_.isLetterOrDigit))
//
//  def contains(char: Char): Predicate[Errors, String] =
//    Predicate.lift(error(s"Must contain the character $char"),
//                   str => str.contains(char))
//
//  def containsOnce(char: Char): Predicate[Errors, String] =
//    Predicate.lift(error(s"Must contain the character $char only once"),
//                   str => str.filter(c => c == char).size == 1)
//
//  /**
//    * ユーザー名とメールアドレスの例では、check()とcheckPred()を異なる状況で使用している点が少し異なります。
//    */
//  val checkUsername: Check[String, String] =
//    checkPred(longerThan(3) and alphanumeric)
//
//  val splitEmail: Check[String, (String, String)] =
//    check(_.split('@') match {
//      case Array(name, domain) =>
//        Right((name, domain))
//
//      case _ =>
//        Left(error("Must contain a single @ character"))
//    })
//
//  val checkLeft: Check[String, String] =
//    checkPred(longerThan(0))
//
//  val checkRight: Check[String, String] =
//    checkPred(longerThan(3) and contains('.'))
//
//  val joinEmail: Check[(String, String), String] =
//    check {
//      case (l, r) =>
//        (checkLeft(l), checkRight(r)).mapN(_ + "@" + _)
//    }
//
//  val checkEmail: Check[String, String] = splitEmail andThen joinEmail
//
//  /**
//    * 最後に、CreateUserの例がKleisliを使って期待通りに動作することがわかります。
//    */
//  final case class User(username: String, email: String)
//
//  def createUser(username: String, email: String): Either[Errors, User] =
//    (
//      checkUsername.run(username),
//      checkEmail.run(email)
//    ).mapN(User)
//
//  createUser("Noel", "noel@underscore.io")
//  // res2: Either[Errors, User] = Right(User("Noel", "noel@underscore.io"))
//  createUser("", "dave@underscore.io@io")
//  // res3: Either[Errors, User] = Left(
//  //   NonEmptyList("Must be longer than 3 characters", List())
//  // )
//
//  /**
//  * これで、コードは完全にKleisliとPredicateで書かれ、Checkは完全に取り除かれました。
//  * これは、ライブラリをシンプルにするための良い第一歩です。
//  * まだまだやるべきことはたくさんありますが、Catsから洗練された構成要素を手に入れることができました。
//  * さらなる改良は読者の皆様にお任せします。
//  *
//  * 10.6 Summary
//  *
//  * 今回のケーススタディでは、抽象化を構築するのではなく、削除する練習をしました。
//  * 最初は、かなり複雑なCheck型から始めました。
//  * 2つの概念を混同していることに気づき、Predicateを分離し、Kleisliで実装できるものにしました。
//  *
//  * 私たちは、合理的な開発者が同意しないような設計上の選択をいくつか行いました。
//  * Predicate を関数に変換するメソッドは、toFunction などではなく、run と呼ぶべきでしょうか。
//  * そもそもPredicateはFunctionのサブタイプであるべきでしょうか？
//  * 多くの関数型プログラマは、暗黙の解決や型推論との相性が悪いため、サブタイプ化を避けたがりますが、ここではサブタイプ化を使うべきだという意見もあるでしょう。
//  * いつものように、最良の決定は、そのライブラリが使用される状況に依存します。
//  */
//}
