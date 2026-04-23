package com.dani.loanservice.exception

import java.util.UUID

class TitleNotFoundException(id: UUID) : RuntimeException("Title not found: $id")