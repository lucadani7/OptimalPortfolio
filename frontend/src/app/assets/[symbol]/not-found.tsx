import Link from "next/link";

export default function AssetNotFound() {
  return (
    <div className="max-w-xl space-y-4 py-12">
      <h1 className="text-[28px] leading-tight font-extrabold tracking-[-0.025em] sm:text-[35px]">No asset with this symbol</h1>
      <p className="text-muted">Check the spelling, or pick the asset from the full list.</p>
      <Link href="/assets" className="inline-flex font-semibold text-steel hover:underline">
        See all assets
      </Link>
    </div>
  );
}
