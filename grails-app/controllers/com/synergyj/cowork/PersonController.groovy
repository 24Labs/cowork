/**
 * Copyright 2002-2010 SynergyJ Servicios Profesionales.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.synergyj.cowork

import com.synergyj.cowork.auth.Person
import grails.plugins.springsecurity.Secured
import com.synergyj.cowork.auth.PersonAuthority
import com.synergyj.cowork.auth.Authority

@Secured(["hasRole('ROLE_OPERATOR')"])
class PersonController {

  def springSecurityService
  def reservationService

  static allowedMethods = [save: "POST", update: "POST", delete: "POST"]

  @Secured(["hasRole('ROLE_USER')"])
  def showAccount = {
    def personId = springSecurityService.principal.id
    redirect action: 'show',id: personId

  }

  @Secured(["hasRole('ROLE_USER')"])
  def showConfirmedReservations = {
    def model = reservationService.obtainReservationsForClient(Long.valueOf(params.id))
    render template: '/reservation/infoTable',model:model
  }

  def index = {
    redirect(action: "list", params: params)
  }

  def list = {
    params.max = Math.min(params.max ? params.int('max') : 10, 100)
    [personInstanceList: Person.list(params), personInstanceTotal: Person.count()]
  }

  def create = {
    def personInstance = new Person()
    personInstance.properties = params
    return [personInstance: personInstance]
  }

  def save = {
    def personInstance = new Person(params)
    // Asignamos las propiedades faltantes
    personInstance.accountExpired = false
    personInstance.accountLocked = false
    personInstance.enabled = true
    personInstance.password = springSecurityService.encodePassword(params.password, params.email)
    if (personInstance.save(flush: true)) {
      //flash.message = "${message(code: 'default.created.message', args: [message(code: 'person.label', default: 'Cliente'), personInstance.id])}"
      def userRole = Authority.findByAuthority("ROLE_USER")
      PersonAuthority.create(personInstance, userRole, true)
      flash.message = "${message(code: 'default.created.message', args: [message(code: 'person.label', default: 'Cliente'), personInstance.razonSocial])}"
      redirect(action: "show", id: personInstance.id)
    }
    else {
      render(view: "create", model: [personInstance: personInstance])
    }
  }

  @Secured(["hasRole('ROLE_OPERATOR') or hasRole('ROLE_USER')"])
  def show = {
    def personInstance = Person.get(params.id)
    if (!personInstance) {
      flash.message = "${message(code: 'default.not.found.message', args: [message(code: 'person.label', default: 'Cliente'), params.id])}"
      redirect(action: "list")
    }
    else {
      [personInstance: personInstance]
    }
  }

  def edit = {
    def personInstance = Person.get(params.id)
    if (!personInstance) {
      flash.message = "${message(code: 'default.not.found.message', args: [message(code: 'person.label', default: 'Cliente'), params.id])}"
      redirect(action: "list")
    }
    else {
      return [personInstance: personInstance]
    }
  }

  def update = {
    def personInstance = Person.get(params.id)
    if (personInstance) {
      if (params.version) {
        def version = params.version.toLong()
        if (personInstance.version > version) {

          personInstance.errors.rejectValue("version", "default.optimistic.locking.failure", [message(code: 'person.label', default: 'Person')] as Object[], "Another user has updated this Person while you were editing")
          render(view: "edit", model: [personInstance: personInstance])
          return
        }
      }
      personInstance.properties = params
      if (!personInstance.hasErrors() && personInstance.save(flush: true)) {
        flash.message = "${message(code: 'default.updated.message', args: [message(code: 'person.label', default: 'Cliente'), personInstance.id])}"
        redirect(action: "show", id: personInstance.id)
      }
      else {
        render(view: "edit", model: [personInstance: personInstance])
      }
    }
    else {
      flash.message = "${message(code: 'default.not.found.message', args: [message(code: 'person.label', default: 'Cliente'), params.id])}"
      redirect(action: "list")
    }
  }

  def delete = {
    def personInstance = Person.get(params.id)
    if (personInstance) {
      try {
        personInstance.delete(flush: true)
        flash.message = "${message(code: 'default.deleted.message', args: [message(code: 'person.label', default: 'Cliente'), params.id])}"
        redirect(action: "list")
      }
      catch (org.springframework.dao.DataIntegrityViolationException e) {
        flash.message = "${message(code: 'default.not.deleted.message', args: [message(code: 'person.label', default: 'Cliente'), params.id])}"
        redirect(action: "show", id: params.id)
      }
    }
    else {
      flash.message = "${message(code: 'default.not.found.message', args: [message(code: 'person.label', default: 'Cliente'), params.id])}"
      redirect(action: "list")
    }
  }
}
