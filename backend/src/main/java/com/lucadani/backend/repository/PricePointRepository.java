package com.lucadani.backend.repository;

import com.lucadani.backend.model.Asset;
import com.lucadani.backend.model.PricePoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface PricePointRepository extends JpaRepository<PricePoint, Long> {
    List<PricePoint> findByAssetAndTradeDateBetweenOrderByTradeDateAsc(Asset asset, LocalDate from, LocalDate to);

    boolean existsByAsset(Asset asset);

    @Query("""
        select new com.lucadani.backend.repository.PriceRow(p.asset.symbol, p.tradeDate, p.close)
        from PricePoint p
        where p.asset.symbol in :symbols and p.tradeDate between :from and :to
        order by p.tradeDate
        """)
    List<PriceRow> findRows(@Param("symbols") Collection<String> symbols,
                            @Param("from") LocalDate from,
                            @Param("to") LocalDate to);

    /** The most recent price of every asset on or before {@code date}. Assets without such a price are absent. */
    @Query("""
        select new com.lucadani.backend.repository.PriceRow(p.asset.symbol, p.tradeDate, p.close)
        from PricePoint p
        where p.asset.symbol in :symbols
          and p.tradeDate = (select max(q.tradeDate) from PricePoint q
                             where q.asset = p.asset and q.tradeDate <= :date)
        """)
    List<PriceRow> findLatestRows(@Param("symbols") Collection<String> symbols,
                                  @Param("date") LocalDate date);
}
