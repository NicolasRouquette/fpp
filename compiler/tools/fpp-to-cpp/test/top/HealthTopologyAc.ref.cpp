// ====================================================================== 
// \title  HealthTopologyAc.cpp
// \author Generated by fpp-to-cpp
// \brief  cpp file for Health topology
//
// \copyright
// Copyright (c) 2021 California Institute of Technology.
// U.S. Government sponsorship acknowledged.
// All rights reserved.
// ======================================================================

#include "HealthTopologyAc.hpp"

namespace Svc {

  namespace {

    // ----------------------------------------------------------------------
    // Component configuration objects
    // ----------------------------------------------------------------------

    namespace ConfigObjects {

      namespace health {
        Svc::HealthImpl::PingEntry pingEntries[] = {
          {
            PingEntries::c1::WARN,
            PingEntries::c1::FATAL,
            c1.getObjName()
          },
          {
            PingEntries::c2::WARN,
            PingEntries::c2::FATAL,
            c2.getObjName()
          },
        }
      }

    }

    // ----------------------------------------------------------------------
    // Component instances
    // ----------------------------------------------------------------------

    // health
    Health health(FW_OPTIONAL_NAME("health"));

    // c1
    C c1(FW_OPTIONAL_NAME("c1"));

    // c2
    C c2(FW_OPTIONAL_NAME("c2"));

  }

}
