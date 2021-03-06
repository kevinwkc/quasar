/*
 * Copyright 2014–2018 SlamData Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package quasar.connector

import slamdata.Predef.{Boolean, Option}
import quasar.api.QueryEvaluator
import quasar.api.datasource.DatasourceType
import quasar.api.resource._

import monocle.{Lens, PLens}

/** @tparam F effects
  * @tparam G multiple results
  * @tparam Q query
  */
trait Datasource[F[_], G[_], Q, R] extends QueryEvaluator[F, Q, R] {

  /** The type of this datasource. */
  def kind: DatasourceType

  /** Returns whether or not the specified path refers to a resource in the
    * specified datasource.
    */
  def pathIsResource(path: ResourcePath): F[Boolean]

  /** Returns the name and type of the `ResourcePath`s implied by concatenating
    * each name to `prefixPath` or `None` if `prefixPath` does not exist.
    */
  def prefixedChildPaths(prefixPath: ResourcePath)
      : F[Option[G[(ResourceName, ResourcePathType)]]]
}

object Datasource {
  def evaluator[F[_], G[_], Q, R]: Lens[Datasource[F, G, Q, R], QueryEvaluator[F, Q, R]] =
    pevaluator[F, G, Q, R, Q, R]

  def pevaluator[F[_], G[_], Q1, R1, Q2, R2]
      : PLens[Datasource[F, G, Q1, R1], Datasource[F, G, Q2, R2], QueryEvaluator[F, Q1, R1], QueryEvaluator[F, Q2, R2]] =
    PLens((ds: Datasource[F, G, Q1, R1]) => ds: QueryEvaluator[F, Q1, R1]) { qe: QueryEvaluator[F, Q2, R2] => ds =>
      new Datasource[F, G, Q2, R2] {
        val kind = ds.kind
        def evaluate(q: Q2) = qe.evaluate(q)
        def pathIsResource(p: ResourcePath) = ds.pathIsResource(p)
        def prefixedChildPaths(pfx: ResourcePath) = ds.prefixedChildPaths(pfx)
      }
    }
}
