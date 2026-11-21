import Link from "next/link";

export default function PortfolioNotFound() {
  return (
    <div className="max-w-xl space-y-4 py-12">
      <h1 className="text-[28px] leading-tight font-extrabold tracking-[-0.025em] sm:text-[35px]">
        This portfolio does not exist
      </h1>
      <p className="text-muted">It may have been deleted. Your other portfolios are still in the list.</p>
      <Link href="/portfolios" className="inline-flex font-semibold text-steel hover:underline">
        See all portfolios
      </Link>
    </div>
  );
}
