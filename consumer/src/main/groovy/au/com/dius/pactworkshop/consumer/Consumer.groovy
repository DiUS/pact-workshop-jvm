package au.com.dius.pactworkshop.consumer

import java.time.LocalDateTime

println new Client('http://localhost:8080').fetchAndProcessData(LocalDateTime.now())
