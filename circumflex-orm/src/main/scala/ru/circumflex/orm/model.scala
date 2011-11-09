package ru.circumflex
package orm

/*!# Data model support

To make Circumflex components independent from various view technologies
we introduce some basic interfaces here. Different components implement
these interfaces while view technologies should provide proper support
for them.
*/
trait Wrapper[T] {
  def item()(implicit ormConf: ORMConfiguration): T
}

/*!# Containers

Containers are generic data-carrier units. They wrap mutable variable
with common functionality like setters, accessors, mutators and metadata.
`ValueHolder` of Circumflex ORM uses container functionality, see its
docs for more information.

By convention containers should be tested for equality by their external
attributes (like name, identifier, etc.), **but not their internal value**.
Implementations should provide sensible `canEqual`, `equal` and `hashCode`
methods, but internal value should not be taken into consideration.
*/
trait Container[T] extends Equals {
  protected var _value: Option[T] = None

  /*!## Setters

  Setters provide a handy mechanism for preprocessing values before
  setting them. They are functions `T => T` which are applied one-by-one
  each time you set new non-null value.
  */
  protected var _setters: Seq[T => T] = Nil
  def setters: Seq[T => T] = _setters
  def addSetter(f: T => T): this.type = {
    _setters ++= List(f)
    this
  }

  /*!## Accessing & Setting Values

  Values are stored internally as `Option[T]`. `None` stands both for
  uninitialized and `null` values.
  */
  def value()(implicit ormConf: ORMConfiguration): Option[T] = _value
  def get()(implicit ormConf: ORMConfiguration) = value
  def apply()(implicit ormConf: ORMConfiguration): T = value.get
  def getOrElse(default: => T)(implicit ormConf: ORMConfiguration): T = value.getOrElse(default)
  def isEmpty()(implicit ormConf: ORMConfiguration): Boolean = value == None

  def set(v: Option[T])(implicit ormConf: ORMConfiguration): this.type = {
    _value = v.map { v =>
      setters.foldLeft(v) { (v, f) => f(v) }
    }
    this
  }
  def set(v: T)(implicit ormConf: ORMConfiguration): this.type = set(core.any2option(v))
  def setNull()(implicit ormConf: ORMConfiguration): this.type = set(None)
  def :=(v: T)(implicit ormConf: ORMConfiguration) = set(v)

  /*!## Methods from `Option`

  Since `ValueHolder` is just a wrapper around `Option`, we provide
  some methods to work with your values in functional style
  (they delegate to their equivalents in `Option`).
  */
  def map[B](f: T => B)(implicit ormConf: ORMConfiguration): Option[B] =
    value.map(f)
  def flatMap[B](f: T => Option[B])(implicit ormConf: ORMConfiguration): Option[B] =
    value.flatMap(f)
  def orElse[B >: T](alternative: => Option[B])(implicit ormConf: ORMConfiguration): Option[B] =
    value.orElse(alternative)

}