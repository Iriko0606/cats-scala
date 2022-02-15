package cats.Chapter8

import scala.concurrent.Future
//import cats.instances.future._ // for Applicative
import cats.instances.list._ // for Traverse
import cats.syntax.traverse._ // for traverse
//import scala.concurrent.ExecutionContext.Implicits.global

object CaseStudyTestingAsynchronousCode extends App {

  /**
    * 8 Case Study: Testing Asynchronous Code
    *
    * ここでは、非同期コードのユニットテストを同期化することで簡単にするという、わかりやすいケーススタディから始めます。
    *
    * 第7章の例に戻って、一連のサーバーの稼働時間を測定することにしましょう。
    * コードをより完全な構造にしていきます。2つのコンポーネントがあります。
    * 1つ目はUptimeClientで、リモートサーバーの稼働時間をポーリングします。
    */
//  trait UptimeClient {
//    def getUptime(hostname: String): Future[Int]
//  }

  /**
    * また、UptimeServiceは、サーバーのリストを管理し、ユーザーがサーバーのトータルアップタイムをポーリングできるようにします。
    */
//  class UptimeService(client: UptimeClient) {
//    def getTotalUptime(hostnames: List[String]): Future[Int] =
//      hostnames.traverse(client.getUptime).map(_.sum)
//  }

  /**
    * UptimeClientをtraitとしてモデル化したのは、ユニットテストでスタブアウトしたいと考えているからです。
    * 例えば、実際のサーバを呼び出すのではなく、ダミーのデータを提供できるテスト用クライアントを書くことができます。
    */
//  class TestUptimeClient(hosts: Map[String, Int]) extends UptimeClient {
//    def getUptime(hostname: String): Future[Int] =
//      Future.successful(hosts.getOrElse(hostname, 0))
//  }

  /**
    * さて、UptimeServiceのユニットテストを書いているとしましょう。
    * どこから値を取得しているかに関わらず、値を合計する能力をテストしたいとします。
    * 以下はその例です。
    */
//  def testTotalUptime() = {
//    val hosts = Map("host1" -> 10, "host2" -> 6)
//    val client = new TestUptimeClient(hosts)
//    val service = new UptimeService(client)
//    val actual: Future[Int] = service.getTotalUptime(hosts.keys.toList)
//    val expected: Int = hosts.values.sum
//    assert(actual == expected)
//    // scala.concurrent.Future[Int] and Int are unrelated: they will most likely never compare equal
//  }

  /**
    * コードがコンパイルされないのは、古典的なエラー12を犯したからです。
    * アプリケーションコードが非同期であることを忘れていたのです。
    * 実際の結果はFuture[Int]型であり、期待される結果はInt型です。
    * この2つを直接比較することはできません。
    *
    * この問題を解決するには、いくつかの方法があります。
    * 非同期性に対応するようにテストコードを変更することができます。
    * しかし、別の方法もあります。サービスのコードを同期させて、テストがそのまま動作するようにしましょう。
    */
  /**
    * 8.1 Abstracting over Type Constructors
    *
    * UptimeClientの2つのバージョンを実装する必要があります。
    * 本番環境で使用する非同期型と、ユニットテストで使用する同期型です。
    */
//  trait RealUptimeClient extends UptimeClient {
//    def getUptime(hostname: String): Future[Int]
//  }
//
//  trait TestUptimeClient extends UptimeClient {
//    def getUptime(hostname: String): Int
//    // Int does not conform to base type String => Future[Int]
//  }

  /**
    * 問題は、UptimeClientの抽象メソッドにどのような結果タイプを与えるべきかということです。
    * 私たちはFuture[Int]とInt.Futureを抽象化する必要があります。
    *
     trait UptimeClient {
       def getUptime(hostname: String): ???
     }
    * 最初は難しく感じるかもしれません。
    * それぞれの型のIntの部分は残したいが、テストコードではFutureの部分を「捨てて」しまいたいのです。
    * 幸いなことに、Catsはセクション4.3で説明したアイデンティティ型であるIdの観点から解決策を提供しています。
    * Idは、型の意味を変えずに型コンストラクタで型を「包む」ことができます。
    */
  type Id[A] = A

  /**
    * Idにより、UptimeClientのリターンタイプを抽象化することができます。
    * これを実装します。
    *
    * - 型コンストラクタF[_]をパラメータとして受け取るUptimeClientの形質定義を書きます。
    * - FをFutureとIdにそれぞれバインドするRealUptimeClientとTestUptimeClientという2つの形質で拡張します。
    * - それぞれのケースでgetUptimeのメソッドシグネチャを書き出し、コンパイルできるかどうかを検証してください。
    *
    * Exercise1
    * 以下が実装です。
    */
  trait UptimeClient[F[_]] {
    def getUptime(hostname: String): F[Int]
  }

  trait RealUptimeClient extends UptimeClient[Future] {
    def getUptime(hostname: String): Future[Int]
  }

//  trait TestUptimeClient extends UptimeClient[Id] {
//    def getUptime(hostname: String): Id[Int]
//  }

  /**
    * もちろん、技術的には、RealUptimeClientやTestUptimeClientでgetUptimeを再宣言する必要はありません。
    * しかし、すべてを書き出すことで、テクニックを説明するのに役立ちます。
    */
  /**
    * なお、Id[A]はAの単なるエイリアスなので、TestUptimeClientの型をId[Int]と表記する必要はありません
    * - 単にIntと表記すればよいのです。

    trait TestUptimeClient extends UptimeClient[Id] {
      def getUptime(hostname: String): Int
    }

    * これで、TestUptimeClient の定義を、先ほどの Map[String, Int]をベースにした完全なクラスにすることができるはずです。
    *
    * Exercise2
    *
    * 最終的なコードは、TestUptimeClientの最初の実装と似ていますが、Future.successfulへの呼び出しが不要になった点が異なります。
    *
    * Exercise2
    **/
  class TestUptimeClient(hosts: Map[String, Int]) extends UptimeClient[Id] {
    def getUptime(hostname: String): Int =
      hosts.getOrElse(hostname, 0)
  }

  /** 8.2 Abstracting over Monads
    *
    * UptimeServiceに注目してみましょう。UptimeClientの2つのタイプを抽象化するように書き換える必要があります。
    * まず、クラスとメソッドのシグネチャを書き換え、次にメソッド本体を書き換えます。
    * まずはメソッドのシグネチャから。
    * - getTotalUptimeのボディをコメントアウトします（すべてをコンパイルするために???にそれを置き換えます)
    * - UptimeServiceにタイプパラメータF[_]を追加し、UptimeClientに渡します。
    *
    * Exercise3
    */
//  class UptimeService[F[_]](client: UptimeClient[F]) {
//    def getTotalUptime(hostnames: List[String]): Future[Int] =
//      ???
////      hostnames.traverse(client.getUptime).map(_.sum)
//  }

  /**
    * 次に、getTotalUptimeのボディのコメントを外します。
    * 以下のようなコンパイルエラーが発生するはずです。
    * // <console>:28: error: could not find implicit value for
    * //               evidence parameter of type cats.Applicative[F]
    * //            hostnames.traverse(client.getUptime).map(_.sum)
    * //                              ^
    *
    * ここでの問題は、traverseはApplicativeを持つ値のシーケンスでしか動作しないことです。元のコードでは、List[Future[Int]]をトラバースしていました。
    * Futureには適用対象があるので、それでよかったのです。
    * このバージョンでは、List[F[Int]]を走査しています。
    * Fが適用可能であることをコンパイラに証明する必要があります。
    * そのためには，UptimeServiceに暗黙のコンストラクタ・パラメータを追加します．
    *
    * Exercise4 */
  import cats.Applicative
  import cats.syntax.functor._
//  class UptimeService[F[_]](client: UptimeClient[F])(
//      implicit applicative: Applicative[F]) {
//    def getTotalUptime(hostnames: List[String]): F[Int] =
//      hostnames.traverse(client.getUptime).map(_.sum)
//  }

  class UptimeService[F[_]: Applicative](client: UptimeClient[F]) {
    def getTotalUptime(hostnames: List[String]): F[Int] =
      hostnames.traverse(client.getUptime).map(_.sum)
  }

  /** あるいは、コンテクストで縛り、より簡潔に以下のようにできます。

    class UptimeService[F[_]: Applicative](client: UptimeClient[F]) {
      def getTotalUptime(hostnames: List[String]): F[Int] =
      hostnames.traverse(client.getUptime).map(_.sum)
    }

    * cats.Applicativeだけでなく、cats.syntax.functorもインポートする必要があることに注意してください。
    * これは、future.mapを使っていたのを、暗黙のFunctorパラメータを必要とするCatsの汎用拡張メソッドに切り替えるためです。

    * 最後に、ユニットテストに目を向けてみましょう。テストコードは何も変更せずに意図した通りに動作するようになりました。
    * TestUptimeClientのインスタンスを作成し、それをUptimeServiceで包みます。
    * これにより、FとIdが効果的に結びつき、残りのコードはモナドやアプリケーションを気にすることなく同期的に動作するようになります。
    * */
  def testTotalUptime() = {
    val hosts = Map("host1" -> 10, "host2" -> 6)
    val client: TestUptimeClient = new TestUptimeClient(hosts)
    val service = new UptimeService(client)
    val actual = service.getTotalUptime(hosts.keys.toList)
    val expected = hosts.values.sum
    assert(actual == expected)
  }

  testTotalUptime()

  /**
  * 8.3 Summary
  * このケーススタディでは、Catsがどのようにして異なる計算シナリオを抽象化するのに役立つのか、その例を紹介します。
  * 非同期と同期のコードを抽象化するためにApplicative型クラスを使用しました。
  * 機能的な抽象化に頼ることで、実装の詳細を気にすることなく、実行したい計算のシーケンスを指定することができます。
  *
  * 図10では、まさにこの種の抽象化を目的とした計算型クラスの「スタック」を示しました。
  * Functor、Applicative、Monad、Traverseなどの型クラスは、マッピング、ジッピング、シーケンシング、イテレーションなどのパターンの抽象的な実装を提供します。
  * これらの型に関する数学的法則は、一貫したセマンティクスのセットで一緒に動作することを保証します。
  *
  * 今回のケーススタディでApplicativeを使用したのは、必要な機能を備えた最も強力でない型クラスだったからです。
  * もし、flatMap を必要としていたら、Applicative を Monad に変更することもできたでしょう。
  * また、異なるシーケンス型を抽象化する必要があれば、Traverse を使用することもできました。
  * また、ApplicativeErrorやMonadErrorのような型クラスもあり、成功した計算だけでなく失敗もモデル化することができます。
  *
  * ここでは、より複雑なケーススタディとして、型クラスを利用した並列処理のためのmap-reduceスタイルのフレームワークを紹介します。
  */
}
