package com.soldout.pagos.repository;

import com.soldout.pagos.entity.PagosSalida;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PagosSalidaRepository extends JpaRepository<PagosSalida, UUID> {

    List<PagosSalida> findByPublicadoFalseOrderByCreadoEnAsc();
}
