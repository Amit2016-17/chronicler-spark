/*
 * Copyright 2018-2019 Faiaz Sanaulla
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

package com.github.fsanaulla.chronicler.spark.structured.streaming

import com.github.fsanaulla.chronicler.core.model.InfluxWriter
import com.github.fsanaulla.chronicler.spark.core.{CallbackHandler, WriteConfig}
import com.github.fsanaulla.chronicler.urlhttp.io.{InfluxIO, UrlIOClient}
import com.github.fsanaulla.chronicler.urlhttp.shared.InfluxConfig
import org.apache.spark.sql.ForeachWriter

import scala.reflect.ClassTag

/**
  * Influx foreach writer for structured streaming
  *
  * @param dbName          - database name
  * @param measName        - measurement name
  * @param wr              - implicit influx writer
  * @param conf            - chronicler influx config
  */
private[streaming] final class InfluxForeachMeasurementWriter[T: ClassTag](
    dbName: String,
    measName: String,
    ch: Option[CallbackHandler],
    wrConf: WriteConfig
)(implicit wr: InfluxWriter[T], conf: InfluxConfig)
    extends ForeachWriter[T] with InfluxForeachWriterBase {

  private[this] var influx: UrlIOClient              = _
  private[this] var meas: UrlIOClient#Measurement[T] = _

  override def open(partitionId: Long, version: Long): Boolean = {
    influx = InfluxIO(conf)
    meas = influx.measurement[T](dbName, measName)
    true
  }

  override def process(value: T): Unit = {
    val response = meas.write(value, wrConf.consistency, wrConf.precision, wrConf.retentionPolicy)
    handleResponse(ch, response)
  }

  override def close(errorOrNull: Throwable): Unit = influx.close()
}
