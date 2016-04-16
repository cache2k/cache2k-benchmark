package org.cache2k.benchmark.thirdparty;

/*
 * #%L
 * thirdparty
 * %%
 * Copyright (C) 2013 - 2016 headissue GmbH, Munich
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import com.github.benmanes.caffeine.cache.simulator.admission.Admission;
import com.github.benmanes.caffeine.cache.simulator.policy.linked.LinkedPolicy;
import com.typesafe.config.ConfigFactory;
import org.cache2k.benchmark.BenchmarkCollection;

/**
 * Benchmarks with LRU from the caffeine simulator. Used to validate
 * the simulator integration.
 *
 * @author Jens Wilke; created: 2013-06-13
 */
public class CaffeineSimulatorLruBenchmark extends BenchmarkCollection {

  {
    factory =
      new CaffeineSimulatorCacheFactory()
        .config(ConfigFactory.empty())
        .policy(_config -> new LinkedPolicy(Admission.ALWAYS, LinkedPolicy.EvictionPolicy.LRU, _config));
  }

}
