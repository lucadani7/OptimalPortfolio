import Link from "next/link";

export default function AssetNotFound() {
  return (
    <div className="space-y-4 py-16 text-center">
      <h1 className="text-2xl font-semibold">Asset not found</h1>
      <p className="text-zinc-600 dark:text-zinc-400">There is no asset with this symbol.</p>
      <Link href="/assets" className="inline-flex text-sm font-medium text-sky-700 hover:underline dark:text-sky-400">
        Back to all assets
      </Link>
    </div>
  );
}
