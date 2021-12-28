package cats.Chapter1

import cats.Show

object Introduction extends App {

  /**
    *Catsには多種多様な関数型プログラミングツールが含まれており、開発者は使いたいものを選んで使用することができます。
    * これらのツールの大部分は、既存のScalaの型に適用できる型クラスの形で提供されています。
    * Catsには多種多様な関数型プログラミングツールが含まれており、開発者は使いたいものを選んで使用することができます。
    * これらのツールの大部分は、既存のScalaの型に適用できる型クラスの形で提供されています。
    *
    * 型クラスは，Haskell2で生まれたプログラミングパターンです．
    * 型クラスは，従来の継承を使わずに，元のライブラリのソースコードを変更することなく，既存のライブラリを新しい機能で拡張することができます。
    *
    * この章では，Underscoreの『Essential Scala』で学んだ型クラスの記憶を取り戻し，Catsのコードベースを初めて見てみましょう。
    * この章では，2つの型クラス（ShowとEq）の例を見て，この本の残りの部分の基礎となるパターンを確認します。
    *
    * 最後に、型クラスを代数的なデータ型、パターンマッチング、値クラス、型エイリアスに結びつけ、Scalaでの関数型プログラミングの構造的なアプローチを紹介します。
    *
    * 1.1 Anatomy of a Type Class
    *
    * タイプ・クラス・パターンには3つの重要な要素があります：タイプ・クラス自体、特定のタイプのためのインスタンス、そしてタイプ・クラスを使用するメソッドです。
    * Scalaの型クラスは、暗黙的な値とパラメータ、そしてオプションとして暗黙的なクラスを使って実装されます。
    * Scalaの言語構成要素は、以下のようにタイプクラスの構成要素に対応しています。
    *
    * traits: type classes;
    * implicit values: type class instances;
    * implicit parameters: type class use; and
    * implicit classes: optional utilities that make type classes easier to use.
    *
    * 詳細を見ていきましょう
    *
    * 1.1.1 The type class
    *
    * 型クラスとは、実装したい機能を表すインターフェースやAPIのことです。
    * Scalaでは、タイプクラスは、少なくとも1つのタイプパラメータを持つtraitで表されます。
    * 例えば，一般的な「JSONへのシリアル化」の動作を次のように表現することができます。
    */
  // Define a very simple JSON AST
  sealed trait Json

  case class JsObject(get: Map[String, Json]) extends Json

  case class JsString(get: String) extends Json

  case class JsNumber(get: Double) extends Json

  case object JsNull extends Json

  // The "serialize to JSON" behaviour is encoded in this trait
  trait JsonWriter[A] {
    def write(value: A): Json
  }

  /**
    * この例では、JsonWriterがタイプクラスで、Jsonとそのサブタイプがサポートコードを提供します。
    * JsonWriterのインスタンスを実装する際には、タイプ・パラメータAに、書き込むデータの具体的な型を指定します。
    *
    * 1.1.2 Type Class Instances
    *
    * 型クラスのインスタンスは、我々が関心を持つ特定の型に対する型クラスの実装を提供します。
    * これには、Scala標準ライブラリの型やドメインモデルの型が含まれます。
    * Scalaでは、型クラスの具体的な実装とともに、暗黙的なキーワドをを作成します。
    */
  final case class Person(name: String, email: String)

  object JsonWriterInstances {
    implicit val stringWriter: JsonWriter[String] =
      new JsonWriter[String] {
        def write(value: String): Json = JsString(value)
      }

    implicit val personWriter: JsonWriter[Person] = {
      new JsonWriter[Person] {
        def write(value: Person): Json =
          JsObject(
            Map(
              "name" -> JsString(value.name),
              "email" -> JsString(value.email)
            ))
      }
    }
    // etc...
  }

  /**
    * これらは暗黙的な値として知られています。
    *
    * 1.1.3 Type Class Use
    *
    * 型クラスの使用とは、型クラスのインスタンスを必要とするすべての機能のことです。
    * Scalaでは、型クラスのインスタンスを暗黙のパラメータとして受け取るすべてのメソッドを意味します。
    *
    * Catsは型クラスを使いやすくするためのユーティリティを提供しており、他のライブラリでもこのようなパターンを見かけることがあります。
    * これには2つの方法があります。
    * インターフェースオブジェクト」と「インターフェース構文」です。
    *
    * Interface Objects
    *
    * 型クラスを使用するインターフェイスを作成する最も簡単な方法は、シングルトン・オブジェクトにメソッドを配置することです。
    */
  import JsonWriterInstances._

  object Json {
    def toJson[A](value: A)(implicit w: JsonWriter[A]): Json = w.write(value)
  }

  /**
    * このオブジェクトを使用するには、気になるタイプクラスのインスタンスをインポートし、関連するメソッドを呼び出します。
    */
  Json.toJson(Person("Dave", "dave@example.com"))
  // res1: Json = JsObject(
  //   Map("name" -> JsString("Dave"), "email" -> JsString("dave@example.com"))
  // )
  /**
    * コンパイラは、暗黙のパラメータを提供せずに toJson メソッドを呼び出したことを発見します。
    * コンパイラはこれを修正するために、関連する型のタイプ・クラス・インスタンスを検索して、呼び出し場所に挿入します。
    */
  Json.toJson(Person("Dave", "dave@example.com"))(personWriter)

  /**
    * Interface Syntax
    *
    * また、既存の型をインターフェースメソッド3で拡張するために、拡張メソッドを使用することもできます。
    * Catsはこれを型クラスの「構文」と呼んでいます。
    */
  object JsonSyntax {

    implicit class JsonWriterOps[A](value: A) {
      // クラスにすることで、受け取り方を柔軟にするかとか決めれるっぽい
      def toJson(implicit w: JsonWriter[A]): Json =
        w.write(value)
    }

  }

  /**
    *インターフェイスの構文は、必要な型のインスタンスと一緒にインポートして使います。
    */
  import JsonWriterInstances._
  import JsonSyntax._

  Person("Dave", "dave@example.com").toJson
  // res3: Json = JsObject(
  //   Map("name" -> JsString("Dave"), "email" -> JsString("dave@example.com"))
  // )
  /**
    * ここでも、コンパイラが暗黙のパラメータの候補を探し、それを埋めてくれます。
    */
  Person("Dave", "dave@example.com").toJson(personWriter)

  /**
    * The implicitly Method
    *
    * Scalaの標準ライブラリには、implicitlyと呼ばれる汎用型クラスのインターフェースが用意されています。
    * その定義は非常にシンプルです。
    */
  def implicitly[A](implicit value: A): A = value

  /**
    * implicitlyを使って、暗黙のスコープから任意の値を呼び出すことができます。
    * 欲しい型を与えれば、あとは暗黙のうちに処理してくれます。
    */
//  implicitly[JsonWriter[String]]
  // res5: JsonWriter[String] = repl.Session$App0$JsonWriterInstances$$anon$1@76f60d45

  /**
    * Catsのほとんどの型クラスは、インスタンスを召喚する別の手段を提供しています。
    * しかし、implicitlyはデバッグのための良い予備手段です。
    * implicitlyの呼び出しをコードの一般的な流れの中に挿入することで、コンパイラが型クラスのインスタンスを見つけられるようにし、曖昧なimplicitエラーが発生しないようにします。
    *
    * 1.2 Working with Implicits
    *
    * Scalaで型クラスを扱うことは、暗黙の値や暗黙のパラメータを扱うことを意味します。
    * これを効果的に行うためには、いくつかのルールを知っておく必要があります。
    *
    * 1.2.1 Packaging Implicits
    *
    * 言語の不思議な特徴で、Scalaで暗黙的とされている定義は、トップレベルではなく、オブジェクトやtraitの中に置かなければなりません。
    * 上の例では、タイプクラスのインスタンスをJsonWriterInstancesというオブジェクトにパッケージしています。
    * JsonWriterのコンパニオン・オブジェクトに配置することも可能です。
    * 型クラスのコンパニオン・オブジェクトにインスタンスを配置することは、暗黙のスコープと呼ばれるものを利用するため、Scalaでは特別な意味を持ちます。
    *
    * 1.2.2 Implicit Scope
    *
    * 上で見たように、コンパイラは型別に型クラスの候補インスタンスを検索します。
    * 例えば、次の式では、JsonWriter[String]型のインスタンスを探します。
    */
//  Json.toJson("A string!")

  /**
    * コンパイラがインスタンスの候補を探す場所を暗黙のスコープといいます。
    * 暗黙のスコープは呼び出しサイトに適用されます。
    * つまり、暗黙のパラメータを持つメソッドを呼び出した時点で適用されます。
    * 暗黙のスコープは、大まかには以下のようになります。
    *
    * - ローカル定義または継承された定義。
    * - インポートされた定義。
    * - 型クラスのコンパニオン・オブジェクトの定義、またはパラメータ型（ここではJsonWriterまたはString）の定義。
    *
    * 定義は、implicit キーワードでタグ付けされている場合にのみ、暗黙のスコープに含まれます。
    * さらに、コンパイラーが複数の定義の候補を見つけた場合、Ambiguous implicit valuesエラーで失敗します。
    */
  implicit val writer1: JsonWriter[String] =
    JsonWriterInstances.stringWriter

  implicit val writer2: JsonWriter[String] =
    JsonWriterInstances.stringWriter

//  Json.toJson("A string")
  // error: ambiguous implicit values:
  // error: ambiguous implicit values:
  //  both value writer1 in object App0 of type => repl.Session.App0.JsonWriter[String]
  //  and value writer2 in object App0 of type => repl.Session.App0.JsonWriter[String]
  //  match expected type repl.Session.App0.JsonWriter[String]
  // Json.toJson("A string")
  // ^^^^^^^^^^^^^^^^^^^^^^^

  /**
    * 暗黙の解決の正確なルールはこれよりも複雑ですが、日常的な使用には複雑さはほとんど関係ありません。
    * 今回の目的のために、タイプクラスのインスタンスをパッケージ化するには、おおよそ次の4つの方法があります。
    * 1 JsonWriterInstancesなどのオブジェクトに配置しています。
    * 2 traitの中に配置する。
    * 3 型クラスのコンパニオン・オブジェクトに配置する。
    * 4 パラメーター型のコンパニオン・オブジェクトに配置することができます。
    *
    * オプション1では、インスタンスをインポートすることでスコープに入れます。
    * オプション2では、継承によってインスタンスをスコープに入れます。
    * オプション3と4では、どこで使おうともインスタンスは常に暗黙のスコープに入ります。
    *
    * 型クラスのインスタンスは、適切な実装が1つしかない場合や、少なくともデフォルトとして広く受け入れられている実装が1つある場合には、コンパニオン・オブジェクトに入れるのが一般的です（上記のオプション3および4）。
    * これにより、型クラスインスタンスを暗黙のスコープに入れるためのインポートが不要となり、使いやすくなります。
    *
    * 1.2.3 Recursive Implicit Resolution
    * 型クラスと暗黙の了解の威力は、コンパイラが暗黙の了解の定義を組み合わせて、インスタンス候補を検索することにあります。
    * これは型クラス構成と呼ばれることもあります。
    * 先ほど、タイプクラスのインスタンスはすべて暗黙のvalであるとほのめかしました。
    * これは単純化したものです。
    * 実際には2つの方法でインスタンスを定義することができます。
    *
    * - 具象インスタンスを必要な型の暗黙のバルとして定義する
    * - 他の型クラスのインスタンスからインスタンスを構築するための暗黙のメソッドを定義する。
    * なぜ、他のインスタンスからインスタンスを構築するのでしょうか？
    * 例えば、Option用のJsonWriterを定義することを考えてみましょう。アプリケーションで気になるAの数だけ、JsonWriter[Option[A]]が必要になります。
    * この問題を解決するために、暗黙的な変数のライブラリを作成して、力技で解決しようとすることもできます。
    */
//  implicit val optionIntWriter: JsonWriter[Option[Int]] =
//    ???
//
//  implicit val optionPersonWriter: JsonWriter[Option[Person]] =
//    ???

  /**
    * しかし，この方法では明らかに拡張性がありません．結局、このアプリケーションでは、すべてのタイプのAに対して、A用とOption[A]用の2つの暗黙のVALが必要になります。
    * 幸いなことに、Option[A]を処理するコードを抽象化して、Aのインスタンスに基づいた共通のコンストラクタにすることができます。
    *
    * - オプションがSome(aValue)の場合，Aのライターを使ってaValueを書きます．
    * - オプションがNoneであれば，JsNullを返します．
    *
    * 以下は，同じコードを暗黙のdefとして書き出したものです．
    */
  implicit def optionWriter[A](
      implicit writer: JsonWriter[A]): JsonWriter[Option[A]] =
    new JsonWriter[Option[A]] {
      def write(option: Option[A]): Json =
        option match {
          case Some(aValue) => writer.write(aValue)
          case None         => JsNull
        }
    }

  /**
    * このメソッドは、Option[A]用のJsonWriterを、A固有の機能を満たす暗黙のパラメータに依存して構築します。
    * コンパイラが次のような式を見てみます。
    */
//  Json.toJson(Option("A string"))

  /**
    * 暗黙の JsonWriter[Option[String]]を探します。JsonWriter[Option[A]]の暗黙的なメソッドを見つけます。
    */
//  Json.toJson(Option("A string"))(optionWriter[String])

  /**
    * で、optionWriterのパラメータとして使用するJsonWriter[String]を再帰的に検索します。
    */
//  Json.toJson(Option("A string"))(optionWriter(stringWriter))

  /**
    * このようにして、暗黙の解決は、暗黙の定義の可能な組み合わせの空間を検索して、正しい全体的な型の型クラスのインスタンスを作成する組み合わせを見つけることになります。
    */
  /**
    * Implicit Conversions
    * 暗黙のdefを使って型クラスのインスタンスコンストラクタを作成するときは、必ずメソッドのパラメータを暗黙のパラメータとしてマークしてください。
    * このキーワードがないと、暗黙の解決の際にコンパイラがパラメータを埋めることができません。
    * 暗黙のパラメータを持つ暗黙のメソッドは，暗黙の変換と呼ばれる別のScalaのパターンを形成します。
    * これは、前のインタフェース構文のセクションとも異なります。
    * なぜなら、その場合、JsonWriterは拡張メソッドを持つ暗黙のクラスだからです。
    * 暗黙の変換は、古いプログラミングパターンであり、現代のScalaコードでは嫌われています。
    * 幸いなことに，暗黙的な変換を行うとコンパイラが警告してくれます。
    * scala.language.implicitConversionsをファイルにインポートして，手動で暗黙の変換を有効にする必要があります。
    */
//  implicit def optionWriter[A](writer: JsonWriter[A]): JsonWriter[Option[A]] =
//    ???

  // warning: implicit conversion method foo should be enabled
  // by making the implicit value scala.language.implicitConversions visible.
  // This can be achieved by adding the import clause 'import scala.language.implicitConversions'
  // or by setting the compiler option -language:implicitConversions.
  // See the Scaladoc for value scala.language.implicitConversions for a discussion
  // why the feature should be explicitly enabled.

  // and so on...

  /**
    * exercise1.3
    * ScalaにはtoStringメソッドがあり、任意の値をStringに変換することができます。
    * しかし，このメソッドにはいくつかの欠点があります：
    * 言語中のすべての型に対して実装されていること，
    * 多くの実装は限定的な使用しかできないこと，
    * 特定の型に対して特定の実装を選択することができないことです．
    *
    * これらの問題を回避するために、Printable型クラスを定義しましょう。
    *
    * 1. Printable[A]という型クラスを定義し、そこに1つのメソッドformatを定義します。
    * formatはA型の値を受け取り、Stringを返します。
    *
    * 2. String および Int に対する Printable のインスタンスを含む PrintableInstances オブジェクトを作成します。
    *
    * 3. 2つの汎用インターフェース・メソッドを持つ Printable オブジェクトを定義します。
    *
    * formatは、A型の値と、それに対応する型のPrintableを受け取ります。
    * format は、A 型の値と、それに対応する型の Printable を受け入れ、関連する Printable を使用して A を String に変換します。
    *
    * print は、format と同じパラメータを受け取り、Unit を返します。
    * println を使用して、フォーマットされた A の値をコンソールに印刷します。
    *
    * これらのステップでは、型クラスの3つの主要コンポーネントを定義します。まず、Printableという型クラス自体を定義します。
    * */
  trait Printable[A] {
    // 1.3_1
    def format(value: A): String
  }

  /**
    * 次に、Printable のデフォルトのインスタンスをいくつか定義し、それらを PrintableInstances にパッケージ化します。
    */
  object PrintableInstances {
    // 1.3_2
    implicit val stringPrintable: Printable[String] =
      new Printable[String] {
        def format(value: String): String = value
      }

    implicit val intPrintable: Printable[Int] =
      new Printable[Int] {
        def format(value: Int): String = value.toString
      }
  }

  /**
    * 最後に、インターフェイス・オブジェクトであるPrintableを定義します。
    */
  object Printable {
    def format[A](value: A)(implicit p: Printable[A]): String = p.format(value)

    def print[A](value: A)(implicit p: Printable[A]): Unit =
      println(format(value))
  }

  /**
    * Using the Library
    *
    * 上記のコードは、複数のアプリケーションで使用できる汎用の印刷ライブラリを形成しています。
    * それでは、このライブラリを利用する「アプリケーション」を定義してみましょう。
    *
    * まず、よく知られている毛皮のような動物を表すデータ型を定義します。
    */
  final case class Cat(name: String, age: Int, color: String)

  /**
    * 次に、次のような形式でコンテンツを返すPrintable for Catの実装を作成します。
    */
//  NAME is a AGE year-old COLOR cat.

// Define a cat:
//  val cat = Cat( /* ... */ )

  // Print the cat!

  /**
    * これは、type classパターンの標準的な使い方です。まず、アプリケーション用のカスタムデータタイプを定義します。
    */
//  final case class Cat(name: String, age: Int, color: String)

  /**
    * そして、気になる型のために型クラスのインスタンスを定義します。
    * これらは、Catのコンパニオン・オブジェクトに入れるか、名前空間として機能するように別のオブジェクトに入れます。
    */
  import PrintableInstances._

  implicit val catPrintable = new Printable[Cat] {
    def format(cat: Cat) = {
      val name = Printable.format(cat.name)
      val age = Printable.format(cat.age)
      val color = Printable.format(cat.color)
      s"$name is a $age year-old $color cat."
    }
  }

  /**
    * 最後に、関連するインスタンスをスコープに入れ、インターフェース・オブジェクト/シンタックスを使用して、タイプ・クラスを使用します。
    * インスタンスをコンパニオン・オブジェクトで定義した場合、Scalaは自動的にそれらをスコープに入れてくれます。
    * そうでない場合は，インポートを使ってアクセスします
    */
  val cat = Cat("Garfield", 41, "ginger and black")
  // cat: Cat = Cat("Garfield", 41, "ginger and black")

  Printable.print(cat)
  // Garfield is a 41 year-old ginger and black cat.

  /**
   * Better Syntax
   *
   * よりよい構文を提供するためにいくつかの拡張メソッドを定義することで、printingライブラリをより使いやすくしましょう。
   *
   * 1. PrintableSyntaxというオブジェクトを作成します。
   *
   * 2. PrintableSyntaxの中に、A型の値をラップする暗黙のクラスPrintableOps[A]を定義します。
   *
   * 3. PrintableOpsには、以下のメソッドを定義します。
   *
   *   - format は、暗黙の Printable[A] を受け入れ、ラップされた A の String 表現を返します。
   *
   *   - printは、暗黙的にPrintable[A]を受け取り、Unitを返します。formatは暗黙のPrintable[A]を受け入れ、ラップされたAの文字列表現を返します。
   *
   * 4. この拡張メソッドを使用して、前の演習で作成した例の Cat を印刷します。
   *
   * まず、拡張メソッドを含む暗黙のクラスを定義します。
   */

  object PrintableSyntax {

    implicit class PrintableOps[A](value: A) {
      def format(implicit p: Printable[A]): String = p.format(value)

      def print(implicit p: Printable[A]): Unit = println(format(p))
    }

  }

  /**
   * PrintableOpsがスコープに入っていれば、ScalaがPrintableの暗黙のインスタンスを見つけることができる任意の値に対して、想像上のprintおよびformatメソッドを呼び出すことができます。
   */

  import PrintableSyntax._

  Cat("Garfield", 41, "ginger and black").print
  // Garfield is a 41 year-old ginger and black cat.

  /**
   * 該当する型のPrintableのインスタンスを定義していない場合、コンパイルエラーが発生します。
   */

//  import java.util.Date
//  new Date().print
  // error: could not find implicit value for parameter p: repl.Session.App0.Printable[java.util.Date]
  // new Date().print
  // ^^^^^^^^^^^^^^^^

  /**
    * 1.4.1 Importing Type Classes
    *
    * Catsの型クラスはcatsパッケージで定義されています。このパッケージから直接Showをインポートすることができます
    */

  /**
    * すべてのCats型クラスのコンパニオン・オブジェクトには、指定した任意の型のインスタンスを検索する適用メソッドがあります。
    */
  import cats.instances.int._
  import cats.instances.string._ // for Show

  //  val showInt = Show.apply[Int]
  // error: could not find implicit value for parameter instance: cats.Show[Int]
  // val showInt:    Show[Int]    = Show.apply[Int]
  //                                ^^^^^^^^^^^^^^^
  /**
    * おっと、うまくいきませんでした。
    * applyメソッドはインプリシットを使って個々のインスタンスを検索するので、いくつかのインスタンスをスコープに入れる必要があります。
    *
    * 1.4.2 Importing Default Instances
    *
    * cats.instancesパッケージは、様々なタイプのデフォルトインスタンスを提供しています。
    * 以下の表のように、これらをインポートすることができます。
    * 各インポートは、特定のパラメータ型に対するすべてのCatsの型クラスのインスタンスを提供します。
    *
    * cats.instances.int provides instances for Int
    * cats.instances.string provides instances for String
    * cats.instances.list provides instances for List
    * cats.instances.option provides instances for Option
    * cats.instances.all provides all instances that are shipped out of the box with Cats
    *
    * 利用可能なインポートの完全なリストについては、cats.instancesパッケージを参照してください。
    *
    * IntとStringのShowをImportしてみましょう。
    */
//  import cats.instances.int._ // for Show
//  import cats.instances.string._ // for Show
//
//  val showInt: Show[Int] = Show.apply[Int]
//  val showString: cats.Show[String] = Show.apply[String]
//
//  val intAsString: String = showInt.show(123)
//  println(intAsString)
//
//  val stringAsString: String = showString.show("abc")
//  println(stringAsString)

  /**
    * 1.4.3 Importing Interface Syntax
    *
    * cats.syntax.showからインターフェイスsyntaxをインポートすることで、Showをより使いやすくすることができます。
    * これにより、スコープ内にShowのインスタンスがあるすべての型に、showという拡張メソッドが追加されます。
    */
  import cats.syntax.show._

  val shownInt = 123.show
  val shownString = "abc".show

  //  println(shownInt)
  //  println(shownString)

  /**
    * Catsでは、型のクラスごとに別々のシンタックス・インポートを用意しています。
    * 後のセクションや章で、これらを紹介していきます。
    *
    * 1.4.4 Importing All The Things!
    * 本書では、特定のインポートを使用して、各例で必要なインスタンスや構文を正確に示します。
    * しかし、これではプロダクションコードに付加価値を与えることはできません。
    * 次のようなインポートを使用した方がシンプルで高速です。
    *
    * - import cats._ Catsのすべての型クラスを一度にインポートします。
    * - import cats.implicits._ 標準的な型クラスのインスタンスと、すべての構文を一度にインポートします。
    *
    * 1.4.5 Defining Custom Instances
    *
    * 与えられた型に対してtraitを実装するだけで、Showのインスタンスを定義することができます。
    */
  import java.util.Date

  //    implicit val dateShow: Show[Date] =
  //      new Show[Date] {
  //        def show(date: Date): String =
  //          s"${date.getTime}ms since the epoch."
  //      }
  //
  //    new Date().show

  /**
    * しかし、Catsはプロセスを簡略化するための便利なメソッドもいくつか用意しています。
    * Showのコンパニオン・オブジェクトには、独自の型のインスタンスを定義するために使える2つの構築メソッドがあります。
    */
  object Show {
    // Convert a function to a `Show` instance:
    def show[A](f: A => String): Show[A] =
      ???

    // Create a `Show` instance from a `toString` method:
    def fromToString[A]: Show[A] =
      ???
  }

  /**
    * これらにより、ゼロから定義するよりも少ない式で素早くインスタンスを構築することができます。
    */
  import java.util.Date

  // これらは、スクラッチでそれらを設定するよりも少ないプロセスでインスタンスを構成できる
  implicit val dateShow: Show[Date] =
    Show.show(date => s"${date.getTime}ms since the epoch.")
  // コンストラクションメソッドを使用したコードコードがないものより簡潔です。
  // Cats の多くの型クラスには、インスタンスを構築するためのヘルパーメソッドが用意されています。(スクラッチあるいは、既存のインスタンスを他の
  // タイプに変化させる)

  import cats.instances.int._
  import cats.instances.string._
  import cats.syntax.show._ // for show

//  final case class Cat(name: String, age: Int, color: String)

  implicit val catShow: Show[Cat] = Show.show[Cat] { cat =>
    val name = cat.name.show
    val age = cat.age.show
    val color = cat.color.show
    s"$name is a $age year-old $color cat."
  }

  //  println(Cat("Rosian", 6, "Gray").show)

  /**
    * 1.5 Example: Eq
  この章の最後に、もう一つの便利な型クラスである cats.Eqを見てみましょう。
   Eq は、型安全な等号化をサポートし、Scalaの組み込み == 演算子を使った
   煩わしさに対処するように設計されています。*/
  import cats.Eq
  import cats.instances.int._
  import cats.instances.long._
  import cats.instances.option._
  import cats.instances.string._
  import java.util.Date

//  List(1, 2, 3).map(Option(_)).filter(item => item == 1)
//  List(1, 2, 3).map(Option(_)).filter(item => item == Some(1))

  /**
   1はOption型と比較しているので2のようにSome(1)と比較しないと、永遠にエラーになる
   けど、==はどのようなオブジェクトのペアに対しても動作するので技術的には型エラーではない
   Eqは等値検査に型の安全性を追加し、この問題を回避するために設計されている
    */
  //  trait Eq[A] {
  //    def eqv(a: A, b: A): Boolean
  //    // other concrete methods based on eqv...
  //  }

  // 1.5.2 Comparing Ints
  val eqInt = Eq[Int]
  //  eqInt.eqv(123, 123)
  //  eqInt.eqv(123, 234)
  /** ↓Scalaの==メソッドとは異なり、eqvを使って異なる型のオブジェクトを比較しようとするとコンパイルエラーが発生します。
  //  eqInt.eqv(123, "234")

  //  cats.syntax.eqのインターフェイス構文をインポートして、===と=!=メソッドを使用することもできます。*/
  import cats.syntax.eq._

  123 === 123
  123 =!= 234

  /** 1.5.3 Comparing Options
  Option[Int]型の値を比較するには、Intと同様にOption用のEqのインスタンスをインポートする必要があります。
   import cats.instances.option._
   ↓の状況だと、value === is not a member of Some[Int]のエラーがでる
   Some(1) === None */
  /**
  ここで、型が完全に一致していないためにエラーが発生しました。
  IntとOption[Int]のスコープにはEqインスタンスがありますが、
  比較している値はSome[Int]型です。この問題を解決するには、
  引数を Option[Int] としてタイプし直す必要があります。
    */
  //  println((Some(1) : Option[Int]) === (None : Option[Int]))
  /**
  標準ライブラリのOption.applyとOption.emptyメソッドを使えば、
  より簡単な方法で行うことができます。
    */
  //  println(Option(1) === Option.empty[Int])
  /**
  cats.syntax.optionの特殊な構文を使用しています。
    */
  //  println(1.some === none[Int])
  // res10: Boolean = false
  //  println(1.some =!= none[Int])
  // res11: Boolean = true

  // 1.5.4 Comparing Custom Types
  /**
    Eq.instanceメソッドは、型 (A, A) => Boolean の関数を受け取り、Eq[A] を返します。
    */
  // for Eq

  implicit val dateEq: Eq[Date] =
    Eq.instance[Date] { (date1, date2) =>
      date1.getTime === date2.getTime
    }

  val x = new Date() // now
  val y = new Date() // a bit later than now
  x === x
  x === y

  /**
    実行中の Cat の例のために Eq のインスタンスを実装します。
    */
//  final case class Cat(name: String, age: Int, color: String)

  /**
    1.5.5 Exercise: Equality, Liberty, and Felinity
    次のオブジェクトのペアの平等と不平等を比較します。
    */
  implicit val catEq: Eq[Cat] =
    Eq.instance[Cat] { (cat1, cat2) =>
      cat1.age === cat2.age && cat1.name === cat2.name && cat1.color === cat2.color
    }
  val cat1 = Cat("Chun", 7, "Red")
  val cat2 = Cat("Yuki", 6, "White")

  val optionCat1 = Option(cat1)
  val optionCat2 = Option.empty[Cat]

  println(optionCat1 === optionCat2) // false
  println(optionCat1 =!= optionCat2) // true

  /**
  1.6.1 Variance(分散)
  型クラスを定義する際には、型パラメータに分散アノテーションを追加して、
  型クラスの分散と暗黙の解決時のコンパイラのインスタンス選択能力に影響を与えることができます。
  Essential Scalaを要約すると、分散はサブタイプに関係しています。
  A型の値を期待している場所であればどこでもB型の値を使うことができれば、
  BはA型のサブ型であると言えます。
  共分散と対分散のアノテーションは、型のコンストラクタで作業するときに発生します。
  例えば，共分散を + 記号で表します．
    */
  //  trait F[+A] // the "+" means "covariant"

  /**
  Covariance(共分散)
  共分散とは、BがAのサブタイプである場合、
  型F[B]は型F[A]のサブタイプであることを意味します。
   これは、ListやOptionのようなコレクションを含む多くのタイプのモデリングに便利です。
    */
  //  trait List[+A]
  //  trait Option[+A]
  /**
  Scalaのコレクションの共分散を利用すると、コードの中で1つのタイプのコレクションをサブタイプのコレクションで置き換えることができます。
  例えば、CircleはShapeのサブタイプなので、List[Shape]を期待している場所であればどこでもList[Circle]を使うことができます。
    */
  //  sealed trait Shape
  //  case class Circle(radius: Double) extends Shape
  //
  //  val circles: List[Circle] = ???
  //  val shapes: List[Shape] = circles
  /**
  一般的に言えば、共分散は出力に使用されます：
  後にListなどのコンテナ型から取り出すことができるデータや、
  そうでなければ何らかのメソッドによって返されるデータです。
    */

  /**
  Contravariance
  Contravarianceはどうでしょうか？ - 記号を使ってContravariance型のコンストラクタを書きます。
    */
  //  trait F[-A]
  /**
  紛らわしいかもしれませんが、Contravarianceとは、AがBのサブタイプである場合、
  型F[B]はF[A]のサブタイプであることを意味します。
  これは、上のJsonWriter型クラスのように入力を表す型をモデル化するのに便利です。
    */
  //  trait JsonWriter[A] {
  //    def write(value: A): Json
  //  }
  /**
  これをもう少し詳しく説明しましょう。
  分散とは、ある値を別の値に置き換える能力のことだということを覚えておいてください。
  2つの値があり、1つはShape型とCircle型で、2つのJsonWritersがあり、1つはShape型、もう1つはCircle型です。
    */
  //  val shape: Shape = ???
  //  val circle: Circle = ???
  //
  //  val shapeWriter: JsonWriter[Shape] = ???
  //  val circleWriter: JsonWriter[Circle] = ???
  //
  //  def format[A](value: A, writer: JsonWriter[A]): Json = writer.write(value)

  /**
  値とライターのどの組み合わせをフォーマットに渡すことができますか？
  サークルはすべてシェイプなので、どちらのライターでもサークルを書くことができます。
  逆に、すべての図形が円ではないので、circleWriterを使って図形を書くことはできません。

  この関係は、コントラバランスを使って正式にモデル化したものです。
  JsonWriter[Shape]はJsonWriter[Circle]のサブタイプです。
  つまり、JsonWriter[Circle]を期待している場所であれば、どこでもshapeWriterを使うことができるということです。
    */

  /**
  Invariance(普遍性)
  Invarianceは説明しやすいものです。+、-を型コンストラクタに書かないものです。
    */
  //  trait F[A]
  /**
  Invariance型F[A]と型F[B]は、AとBの関係がどのようなものであっても、
  お互いのサブ型になることはないということを意味します。
  コンパイラは暗黙的なものを探すとき、型またはサブタイプにマッチするものを探します。
  このように、分散アノテーションを使用して、型クラスのインスタンス選択をある程度制御することができます。
  頻発する問題が2つあります。代数的なデータ型あることを考えてみましょう
    */
  //  sealed trait A
  //  final case object B extends A
  //  final case object C extends A
  /**
  このような問題があります。
  1. スーパータイプに定義されたインスタンスが利用可能な場合、そのインスタンスは選択されますか？
    例えば、Aのインスタンスを定義して、B型とC型の値で動作させることはできますか？

  2. サブタイプのインスタンスは、スーパータイプのインスタンスよりも優先して選択されますか？
    例えば、AとBのインスタンスを定義していて、B型の値がある場合、BのインスタンスはAよりも優先して選択されますか？

  両方を同時に持つことはできないことがわかりました。3つの選択肢は以下のような行動を与えてくれます。
  | Type Class Variance           |	Invariant(F[A]) |	Covariant(F[+A]) |	Contravariant(F[-A]) |
  | Supertype instance used?	    |       No	      |       No	       |          Yes          |
  | More specific type preferred?	|       No        |       Yes        |          No           |

  完璧なシステムがないことは明らかです。
  Catsは不変型クラスを使うことを好みます。
  これにより、必要に応じてサブタイプに対してより具体的なインスタンスを指定することができます。
  これは、例えば、Some[Int]型の値を持っている場合、Option用の型クラスのインスタンスは使用されないことを意味します。
  この問題は、Some(1) : Option[Int] のような型のアノテーションや、
  1.5.3節で説明した Option.apply, Option.empty, some, and none のような
  「スマートなコンストラクタ」を使用することで解決できます。
    */

  /**
  1.7 Summary
  この章では、まず型クラスについて見ていきました。Cats-ShowとEqの2つの例を見る前に、
  プレーンなScalaを使って独自のPrintable型クラスを実装しました。

  型クラスを構成する要素を見ました。
  - 型クラスである trait
  - 暗黙の値である型クラスインスタンス。
  - 暗黙のパラメータを使用する型クラスの使用法。

  キャッツ型クラスの一般的なパターンも見てきました。
  - 型クラス自体は cats パッケージの汎用的な形質です。
  - 各型クラスは、インスタンスを実体化するための apply メソッド、インスタンスを作成するための
    1つ以上の construction メソッド、およびその他の関連するヘルパーメソッドのコレクションを持つコンパニオンオブジェクトを持っています。
  - デフォルトのインスタンスは cats.instances パッケージ内のオブジェクトを介して提供され、
    型クラスごとではなくパラメータの型ごとに整理されています。
  - 多くの型クラスは cats.syntax パッケージを介して提供される構文を持っています。

  第1部の残りの章では、セミグループ、モノイド、ファンクタ、モナド、セミグループール、アプリカティブ、トラバースなどの広範で強力な型クラスを見ていきます。
  それぞれのケースで、型クラスがどのような機能を提供しているのか、形式的な規則に従っているのか、そしてそれがどのようにCatsで実装されているのかを学びます。
  これらの型クラスの多くは、ShowやEqよりも抽象度が高くなっています。
  */
}
