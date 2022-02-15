package cats.Chapter5

import cats.data.{EitherT}

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

object MonadTransformers extends App {

  /**
    * 5 Monad Transformers
    *
    * モナドはブリトーのようなもので、一度味を覚えると何度もリピートすることになります。
    * これには問題がないわけではありません。
    * ブリトーがウエストを膨らませるように、モナドは入れ子になったfor-comprehensions(for {} yield())によってコードベースを膨らませます。
    *
    * 私たちがデータベースを操作しているとします。
    * ユーザーのレコードを検索したいとします。
    * ユーザーが存在するかどうかわからないので、Option[User]を返します。
    * データベースとの通信はさまざまな理由で失敗する可能性があります（ネットワークの問題、認証の問題など）。
    * そのため、この結果はEitherにまとめられ、最終的な結果はEither[Error, Option[User]]となります。
    *
    * この値を使用するには、flatMapの呼び出し（または同等のfor-comprehensions）をネストする必要があります。
    */
//  def lookupUserName(id: Long): Either[Error, Option[String]] =
//    for {
//      optUser <- lookupUser(id)
//    } yield {
//      for { user <- optUser } yield user.name
//    }

  /**
    * これはすぐに非常に面倒なことになります。
    *
    * 5.1 Exercise: Composing Monads
    *
    * ある疑問が出てきます。任意の2つのモナドが与えられたとき、それらを何らかの方法で組み合わせて1つのモナドにすることはできるのか？
    * つまり、モナドは合成できるのか？
    * 実際にコードを書いてみても、すぐに問題が発生します。
    */
  import cats.syntax.applicative._ // for pure

  // Hypothetical example. This won't actually compile:
//  def compose[M1[_]: Monad, M2[_]: Monad] = {
//    type Composed[A] = M1[M2[A]]
//
//    new Monad[Composed] {
//      def pure[A](a: A): Composed[A] =
//        a.pure[M2].pure[M1]
//
//      def flatMap[A, B](fa: Composed[A])(f: A => Composed[B]): Composed[B] =
//        // Problem! How do we write flatMap?
//        ???
//    }
//  }

  /**
    * M1やM2について何も知らずにflatMapの一般的な定義を書くことは不可能です。
    * しかし、どちらか一方のモナドについて何か知っていれば、一般的にこのコードを完成させることができる。
    * 例えば、上記のM2をOptionに固定すると、flatMapの定義が見えてきます。
    */
//  def flatMap[A, B](fa: Composed[A])(f: A => Composed[B]): Composed[B] =
//    fa.flatMap(_.fold[Composed[B]](None.pure[M1])(f))

  /**
    * 上記の定義では、一般的なモナドのインターフェースにはない Option 固有の概念である None を使用していることに注意してください。
    * Optionを他のモナドと組み合わせるには、このような特別な概念が必要です。
    * 同様に、他のモナドについても、構成されたフラットマップ・メソッドを書くのに役立つものがあります。
    * これがモナド・トランスフォーマーの考え方です。
    * Catsは様々なモナドのトランスフォーマーを定義しており、それぞれがそのモナドを他のモナドと組み合わせるために必要な追加の知識を提供しています。
    * いくつかの例を見てみましょう。
    *
    * 5.2 A Transformative Example
    *
    * Catsは多くのモナドのtransformerを提供しており、それぞれにTという接尾辞がついています。
    * EitherTはEitherと他のモナドを合成し、OptionTはOptionを合成する、といった具合です。
    *
    * OptionTを使ってListとOptionを合成する例を紹介します。
    * OptionT[List, A]（便宜上，ListOption[A]と呼ばれる）を使って，List[Option[A]]を単一のモナドに変換することができます。
    */
  import cats.data.OptionT

//  type ListOption[A] = OptionT[List, A]

  /**
    * 外側のモナドの型である List を，内側のモナドのtransformerである OptionT のパラメータとして渡しています。
    * ListOptionのインスタンスはOptionTのコンストラクタを使って作成できますが，もっと便利なのはpureを使用することです。
    */
  import cats.instances.list._ // for Monad
  import cats.syntax.applicative._ // for pure

  val result1: ListOption[Int] = OptionT(List(Option(10)))
  // result1: ListOption[Int] = OptionT(List(Some(10)))

  val result2: ListOption[Int] = 32.pure[ListOption]
  // result2: ListOption[Int] = OptionT(List(Some(32)))

  /**
    * mapおよびflatMapメソッドは、ListおよびOptionの対応するメソッドを単一の操作にまとめたものです。
    */
  result1.flatMap { (x: Int) =>
    result2.map { (y: Int) =>
      x + y
    }
  }
  // res1: OptionT[List, Int] = OptionT(List(Some(42)))

  /**
    * これは、すべてのモナド変換の基本です。
    * mapメソッドとflatMapメソッドを組み合わせることで、計算の各段階で値を再帰的にアンパックしたりリパックしたりすることなく、両方のコンポーネントモナドを使用することができます。
    * それでは、APIをさらに詳しく見ていきましょう。
    *
    * Complexity of Imports
    *
    * 上のコードサンプルのインポートは、すべてがどのように組み合わされているかを示しています。
    *
    * cats.syntax.applicativeをインポートして純粋な構文を取得します。pureはApplicative[ListOption]型の暗黙のパラメータを必要とします。
    * まだApplicativeには出会っていませんが、すべてのMonadsはApplicativeでもあるので、今はその違いを無視してもいいでしょう。
    *
    * Applicative[ListOption]を生成するためには，List用のApplicativeとOptionTのインスタンスが必要です．
    * OptionT は Cats のデータ型なので，そのインスタンスはコンパニオン・オブジェクトから提供されます．
    * List用のインスタンスはcats.instances.listから得られます。
    *
    * cats.syntax.functorやcats.syntax.flatMapをインポートしていないことに注目してください。
    * これは、OptionTが具象データ型であり、独自の明示的なMapメソッドとFlatMapメソッドを持っているからです。
    * 構文をインポートしても問題はありません。コンパイラはそれを無視して明示的なメソッドを優先します。
    *
    * 私たちがこのような悪ふざけをしているのは、普遍的な Cats のインポートである cats.implicits を頑なに使おうとしないからだということを忘れないでください。
    * もしこのインポートを使えば、必要なインスタンスや構文はすべてスコープ内に収まり、すべてがうまくいくはずです。
    *
    * 5.3 Monad Transformers in Cats
    *
    * 各モナド変換器は cats.data で定義されたデータ型で、モナドのスタックをラップして新しいモナドを生成することができます。
    * 作成したモナドは、モナド型クラスを介して使用します。
    * モナド変換を理解するために必要な主な概念は、以下の通りです。
    *
    * - 有効なトランスフォーマークラスです。
    * - transformerを使ってモナドスタックをどのように作るか
    * - どのようにモナドのインスタンスを構成するか、そして
    * - どのようにモナドにラップされ、分解されたスタックにアクセスするか
    *
    * 5.3.1 The Monad Transformer Classes
    *
    * 慣習的に、CatsではFooというモナドはFooTというトランスフォーマークラスを持ちます。
    * 実際、Catsの多くのモナドは、モナドトランスフォーマとIdモナドを組み合わせて定義されています。
    * 具体的には、以下のようなものがあります。
    *
    * - cats.data.OptionT for Option;
    * - cats.data.EitherT for Either;
    * - cats.data.ReaderT for Reader;
    * - cats.data.WriterT for Writer;
    * - cats.data.StateT for State;
    * - cats.data.IdT for the Id monad.
    *
    * Kleisli Arrows
    *
    * セクション4.8では、Readerモナドが、Catsではcats.data.Kleisliとして表現される「クライスリ・アロー」と呼ばれるより一般的な概念を特殊化したものであることを述べました。
    * KleisliとReaderTは、実は同じものであることがわかりました。ReaderTは、実はクライスリの型の別名なのです。
    * したがって、前章でリーダーを作成し、コンソールにクライスリが表示されていました。
    *
    * 5.3.2 Building Monad Stacks
    *
    * これらのモナドtransformerは、すべて同じ規則に従っています。
    * transformer自体はスタックの中の内側のモナドを表し、最初の型パラメータは外側のモナドを指定します。
    * 残りの型パラメータは、対応するモナドを形成するために使用した型です。
    *
    * 例えば、上記のListOption型はOptionT[List, A]の別名ですが、結果は実質的にList[Option[A]]となります。
    * 言い換えれば，モナドスタックを内側から構築するということです。
    */
  type ListOption[A] = OptionT[List, A]

  /**
    * 多くのモナドやすべてのtransformerは、少なくとも2つの型パラメータを持っているので、中間段階で型のエイリアスを定義しなければならないことがよくあります。
    *
    * 例えば、OptionにEitherを巻き付けたいとします。
    * Optionは最も内側にある型なので、OptionTモナド変換器を使います。
    * そのためには、最初の型パラメーターとしてEitherを使う必要があります。
    * しかし、Either自体には2つの型パラメータがあり、モナドには1つしかありません。
    * 型コンストラクタを正しい形に変換するために、型エイリアスが必要です。
    */
  // Alias Either to a type constructor with one parameter:
  type ErrorOr[A] = Either[String, A]

  // Build our final monad stack using OptionT:
  type ErrorOrOption[A] = OptionT[ErrorOr, A]

  /**
    * ErrorOrOptionは，ListOptionと同様にモナドです。
    * インスタンスの生成や変換には，通常通り pure, map, flatMap を使用できます．
    */
  import cats.instances.either._ // for Monad

  val a = 10.pure[ErrorOrOption]
  // a: ErrorOrOption[Int] = OptionT(Right(Some(10)))
  val b = 32.pure[ErrorOrOption]
  // b: ErrorOrOption[Int] = OptionT(Right(Some(32)))

  val c = a.flatMap(x => b.map(y => x + y))
  // c: OptionT[ErrorOr, Int] = OptionT(Right(Some(42)))

  /**
    * 3つ以上のモナドを重ねようとすると、事態はさらに混乱します。
    *
    * 例えば、OptionのEitherのFutureを作ってみましょう。
    * 繰り返しになりますが、FutureのEitherTのOptionTを使って内側から作ります。
    * しかし、EitherTには3つの型パラメータがあるので、1行では定義できません。
    */
//  case class EitherT[F[_], E, A](stack: F[Either[E, A]]) {
//    // etc...
//  }

  /**
    * 3つのタイプパラメータは以下の通りです。
    *
    * - F[_]はスタックの外側のモナドです（Eitherは内側）
    * - E は Either のエラー型です。
    * - Aは、Eitherの結果型です。
    *
    * 今回は、FutureとErrorを修正し、Aを変化させるEitherTの別名を作成します。
    */
  import scala.concurrent.Future
  import cats.data.OptionT

  type FutureEither[A] = EitherT[Future, String, A]

  type FutureEitherOption[A] = OptionT[FutureEither, A]

  /**
    * マンモススタックは3つのモナドを構成し、mapメソッドとflatMapメソッドは3つの抽象化されたレイヤーを通過します。
    */
//  val futureEitherOr: FutureEitherOption[Int] =
//    for {
//      a <- 10.pure[FutureEitherOption]
//      b <- 32.pure[FutureEitherOption]
//    } yield a + b

  /**
    * Kind Projector
    *
    * モナドスタックを構築する際に複数の型エイリアスを定義することが多い場合、Kind Projectorコンパイラプラグインを試してみてはいかがでしょうか。
    * Kind ProjectorはScalaの型構文を強化して、部分的に適用される型コンストラクタを簡単に定義できるようにします。例えば
    */
//
////  123.pure[EitherT[Option, String, *]]
////  // res3: EitherT[Option, String, Int] = EitherT(Some(Right(123)))

  /**
    * Kind Projectorはすべての型宣言を1行にまとめることはできませんが、
    * コードを読みやすくするために必要な中間型定義の数を減らすことができます。
    *
    * 5.3.3 Constructing and Unpacking Instances
    *
    * 上で見たように、モナドスタックを作成するには、
    * 関連するモナドtransformerのapplyメソッドを使用するか、通常のPureな構文を使用することができます。
    */
  // Create using apply:
  val errorStack1 = OptionT[ErrorOr, Int](Right(Some(10)))
  // errorStack1: OptionT[ErrorOr, Int] = OptionT(Right(Some(10)))

  // Create using pure:
  val errorStack2 = 32.pure[ErrorOrOption]
  // errorStack2: ErrorOrOption[Int] = OptionT(Right(Some(32)))

  /**
    * モナドtransformerスタックの処理が終わったら、そのvalueメソッドを使ってアンパックすることができます。
    * これにより、変換されていないスタックが返されます。
    * その後、通常の方法で個々のモナドを操作することができます。
    */
  // Extracting the untransformed monad stack:
//  errorStack1.value
  // res4: ErrorOr[Option[Int]] = Right(Some(10))

  // Mapping over the Either in the stack:
//  errorStack2.value.map(_.getOrElse(-1))
  // res5: Either[String, Int] = Right(32)


  /**
    * valueを呼び出すたびに、1つのモナドtransformerがアンパックされます。
    * 大きなスタックを完全にアンパックするには、複数回の呼び出しが必要な場合があります。
    * 例えば、上記のFutureEitherOptionスタックを待機させるためには、valueを2回呼び出す必要があります。
    */
//  futureEitherOr
//   res6: FutureEitherOption[Int] = OptionT(
//     EitherT(Future(Success(Right(Some(42)))))
//   )

//  val intermediate = futureEitherOr.value
  // intermediate: FutureEither[Option[Int]] = EitherT(
  //   Future(Success(Right(Some(42))))
  // )

//  val stack = intermediate.value
  // stack: Future[Either[String, Option[Int]]] = Future(Success(Right(Some(42))))

//  Await.result(stack, 1.second)
  // res7: Either[String, Option[Int]] = Right(Some(42))

  /**
    * 5.3.4 Default Instances
    *
    * Catsの多くのモナドは、対応するトランスフォーマーとIdモナドを使って定義されています。
    * これは、モナドとトランスフォーマーのAPIが同一であることを確認する意味でも心強い。
    * Reader、Writer、Stateはすべてこのように定義されています。
    */
//  type Reader[E, A] = ReaderT[Id, E, A] // = Kleisli[Id, E, A]
//  type Writer[W, A] = WriterT[Id, W, A]
//  type State[S, A] = StateT[Id, S, A]

  /**
    * また、モナド変換器が対応するモナドとは別に定義されている場合もあります。
    * このような場合、トランスフォーマーのメソッドはモナドのメソッドを反映する傾向があります。
    * たとえば、OptionTはgetOrElseを定義し、EitherTはfold、bimap、swap、その他の有用なメソッドを定義しています。
    *
    * 5.3.5 Usage Patterns
    * モナド変換器は、モナドをあらかじめ定義された方法で融合させるため、広く使用することが難しい場合があります。
    * 慎重に考えないと、異なる文脈でモナドを操作するために、モナドを異なる構成で解凍したり再構成したりしなければならないことになります。
    *
    * この問題に対処するには、さまざまな方法があります。1つの方法は、1つの「スーパースタック」を作成し、コードベース全体にそれを適用することです。
    * これは、コードが単純で、性質がほぼ同じである場合に有効です。
    * 例えば、ウェブアプリケーションでは、全てのリクエストハンドラは非同期であり、全てのリクエストハンドラは同じHTTPエラーコードで失敗すると決めることができます。
    * エラーを表すカスタムADT(代替的データ: abstract data type)を設計し、コードのいたるところでFutureとEitherを融合して使用することができます。
    */
  sealed abstract class HttpError
  final case class NotFound(item: String) extends HttpError
  final case class BadRequest(msg: String) extends HttpError
  // etc...

//  type FutureEither[A] = EitherT[Future, HttpError, A]

  /**
    * スーパースタックアプローチは、大規模で異質なコードベースでは、異なるスタックが異なる文脈で意味を持つようになると失敗し始めます。
    * このような状況では、モナド変換をローカルな「グルーコード」として使用するデザインパターンが有効です。
    * 変換されていないスタックをモジュール境界で公開し、ローカルで操作するために変換し、渡す前に変換を解除します。
    * これにより、コードの各モジュールは、どのトランスフォーマーを使用するかを独自に決定することができます。
    */
  import cats.data.Writer

  type Logged[A] = Writer[List[String], A]

  // Methods generally return untransformed stacks:
  def parseNumber(str: String): Logged[Option[Int]] =
    util.Try(str.toInt).toOption match {
      case Some(num) => Writer(List(s"Read $str"), Some(num))
      case None      => Writer(List(s"Failed on $str"), None)
    }

//   Consumers use monad transformers locally to simplify composition:
  def addAll(a: String, b: String, c: String): Logged[Option[Int]] = {
    import cats.data.OptionT

    // flatMapでaまで取れる
    val result = for {
      a <- OptionT(parseNumber(a))
      b <- OptionT(parseNumber(b))
      c <- OptionT(parseNumber(c))
    } yield a + b + c

    result.value
  }

//  // This approach doesn't force OptionT on other users' code:
//  val result1 = addAll("1", "2", "3")
//   result1: Logged
//     [Option[Int]] = WriterT(
//     (List("Read 1", "Read 2", "Read 3"), Some(6))
//   )
//  val result2 = addAll("1", "a", "3")
//   result2: Logged[Option[Int]] = WriterT(
//     (List("Read 1", "Failed on a"), None)
//   )

  /**
    * 残念ながら、モナド変換には万能のアプローチはありません。チームの規模や経験、コードベースの複雑さなど、さまざまな要因によって最適な方法は変わってきます。
    * モナド変換が適しているかどうかを判断するには、実験したり、同僚からのフィードバックを集めたりする必要があるでしょう。
    *
    * 5.4 Exercise: Monads: Transform and Roll Out
    *
    *  変装したロボットとして知られるオートボットは、戦闘中に頻繁にメッセージを送り、チームの仲間のパワーレベルを確認します。
    *  これにより、戦略の調整や破壊的な攻撃を行うことができます。
    *  メッセージの送信方法は次のようになっています。
    */
//  def getPowerLevel(autobot: String): Response[Int] =
//    ???

  /**
    * 地球の粘性の高い大気中では送信に時間がかかり、衛星の故障や厄介なディセプティコンによる妨害行為などでメッセージが失われることもあります。
    * そのため、応答はモナドのスタックとして表現されます。
    */
  // なんとなくわかった
//  type Response[A] = Future[Either[String, A]]

  /**
    * オプティマス・プライムは、神経マトリックスの入れ子式のfor comprehensionに飽きています。
    * モナド変換を使ってResponseを書き換えることで、彼を助けてあげてください。
    */
  // Exercise 5.4.1
  type Response[A] = EitherT[Future, String, A]

  /**
    * それでは、getPowerLevelを実装して、架空の味方からデータを取得し、コードをテストしてみましょう。
    * 使用するデータは以下の通りです。
    */
  val powerLevels = Map(
    "Jazz" -> 6,
    "Bumblebee" -> 8,
    "Hot Rod" -> 10
  )

  /**
    * AutobotがpowerLevelsマップにない場合、到達できなかったことを報告するエラーメッセージを返します。
    * メッセージには名前を入れると効果的です。
    */
  // Exercise 5.4.2
  import cats.data.EitherT
  import scala.concurrent.Future
  import cats.instances.future._ // for Monad
  import scala.concurrent.ExecutionContext.Implicits.global

  // 答え見た match case
  def getPowerLevel(ally: String): Response[Int] = {
    powerLevels.get(ally) match {
      case Some(avg) => EitherT.right(Future(avg))
      case None      => EitherT.left(Future(s"$ally unreachable"))
    }
  }

  /**
    * 2体のオートボットは、そのパワーレベルの合計が15以上であれば、必殺技を繰り出すことができます。
    * 2人の味方の名前を受け取り、必殺技が可能かどうかをチェックする2つ目のメソッド、canSpecialMoveを書きます。
    * どちらかの味方が利用できない場合は、適切なエラーメッセージを表示して失敗します。
    */
  // これはなんとなくできた
  def canSpecialMove(ally1: String, ally2: String): Response[Boolean] =
    for {
      power1 <- getPowerLevel(ally1)
      power2 <- getPowerLevel(ally2)
    } yield power1 + power2 >= 15

  /**
    * 最後に、2人の味方の名前を受け取り、その味方が必殺技を使えるかどうかのメッセージを表示するメソッドtacticalReportを書きます。
    */
  def tacticalReport(ally1: String, ally2: String): String = {
    // ここまでしかわからなかった。
    val special = canSpecialMove(ally1, ally2).value

    Await.result(special, 1.second) match {
      case Left(msg) =>
        s"Comms error: $msg"
      case Right(true) =>
        s"$ally1 and $ally2 are ready to roll out!"
      case Right(false) =>
        s"$ally1 and $ally2 need a recharge."
    }

  }

  /**
    *次のようにレポートを使用することができるはずです。
    */
  println(tacticalReport("Jazz", "Bumblebee"))
  // res13: String = "Jazz and Bumblebee need a recharge."
  println(tacticalReport("Bumblebee", "Hot Rod"))
  // res14: String = "Bumblebee and Hot Rod are ready to roll out!"
  println(tacticalReport("Jazz", "Ironhide"))
  // res15: String = "Comms error: Ironhide unreachable"

  /**
   * 5.5 Summary
   *
   * この章では、モナドtransformersを紹介しました。
   * モナド変換器は、ネストされたモナドの「スタック」を扱う際に、ネストされた内包やパターン・マッチングを必要としません。
   *
   * FutureT、OptionT、EitherTなどの各モナドトランスフォーマは、その関連モナドを他のモナドにマージするために必要なコードを提供します。
   * トランスフォーマーは、モナドスタックを包むデータ構造で、スタック全体を展開・再パックするmapおよびflatMapメソッドを備えています。
   *
   * モナド変換の型シグネチャは内側から外側に向かって書かれているので、EitherT[Option, String, A]はOption[Either[String, A]]のラッパーになります。
   * 深く入れ子になったモナドの変換型を書くときには、型の別名を使うと便利です。
   *
   * 今回のモナド変換で、モナドとflatMapを使った計算の順序付けについて、知っておくべきことをすべて網羅しました。
   * 次の章では趣向を変えて、文脈の中で独立した値をジッピングするなどの新しい操作をサポートする2つの新しい型クラス、SemigroupalとApplicativeについて説明します。
   */
}
