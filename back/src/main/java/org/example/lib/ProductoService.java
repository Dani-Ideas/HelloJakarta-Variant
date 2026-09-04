package org.example.lib;

// El "general" -- lo unico que inyecta el Resource. No agrega metodos propios, solo junta
// los dos contratos; quien decide si una llamada es de lectura o escritura es la
// implementacion (ProductoServiceImpl, en ejb/), no esta interfaz.
public interface ProductoService extends ProductoReadService, ProductoWriteService {
}
