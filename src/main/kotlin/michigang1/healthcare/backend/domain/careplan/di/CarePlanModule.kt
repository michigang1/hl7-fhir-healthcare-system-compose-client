package michigang1.healthcare.backend.domain.careplan.di

import michigang1.healthcare.backend.domain.careplan.service.CarePlanService
import michigang1.healthcare.backend.domain.careplan.service.impl.CarePlanServiceImpl
import org.koin.dsl.module

val carePlanModule = module {
    single<CarePlanService> { CarePlanServiceImpl() }
}